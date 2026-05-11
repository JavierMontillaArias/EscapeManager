from fastapi import HTTPException, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from jose import jwt, JWTError
from sqlalchemy.orm import Session
from starlette import status

from app.config import settings
from app.database import get_db
from app.models.user import User

oauth2_scheme = HTTPBearer()

ROLE_MANAGER = "Manager"
ROLE_GAMEMASTER = "Game Master"


def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(oauth2_scheme), db: Session = Depends(get_db)) -> User:
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="No se pudieron validar las credenciales",
        headers={"WWW-Authenticate": "Bearer"},
    )

    try:
        token = credentials.credentials  # extrae el token del objeto
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        user_id_str: str | None = payload.get("sub")

        if user_id_str is None:
            raise credentials_exception

        token_type: str | None = payload.get("type")
        if token_type != "access":
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Se requiere un access token, no un refresh token",
                headers={"WWW-Authenticate": "Bearer"},
            )

        user_id = int(user_id_str)

    except (JWTError, ValueError):
        raise credentials_exception

    user = (
        db.query(User)
        .filter(User.id == user_id, User.activo == True)
        .first()
    )
    if user is None:
        raise credentials_exception

    return user


def require_manager(current_user: User = Depends(get_current_user)) -> User:
    if current_user.rol.nombre != ROLE_MANAGER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Esta acción requiere rol Manager",
        )
    return current_user


def require_gamemaster(current_user: User = Depends(get_current_user)) -> User:
    if current_user.rol.nombre != ROLE_GAMEMASTER:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Esta acción requiere rol Game Master",
        )
    return current_user


def require_manager_or_gamemaster(current_user: User = Depends(get_current_user)) -> User:
    allowed_roles = {ROLE_MANAGER, ROLE_GAMEMASTER}
    if current_user.rol.nombre not in allowed_roles:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Acceso no autorizado",
        )
    return current_user