from datetime import timedelta, timezone, datetime
from typing import Optional

from jose import jwt, JWTError
from passlib.context import CryptContext
from sqlalchemy.orm import Session

from app.config import settings
from app.models import User, Role
from app.schemas import RegisterRequest

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def hash_password(password: str) -> str:
    return pwd_context.hash(password)

def verify_password(plain_password: str, hashed_password: str) -> bool:
    return pwd_context.verify(plain_password, hashed_password)

def _create_token(data: dict, expires_delta: timedelta, token_type: str) -> str:
    to_encode = data.copy()
    expire = datetime.now(timezone.utc) + expires_delta
    to_encode.update({"exp": expire, "type": token_type})
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)

def create_access_token(user_id: int) -> str:
    return _create_token(
        data={"sub": str(user_id)},
        expires_delta=timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES),  # minutos
        token_type="access",
    )

def create_refresh_token(user_id: int) -> str:
    return _create_token(
        data={"sub": str(user_id)},
        expires_delta=timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS),
        token_type="refresh",
    )

def decode_refresh_token(token: str) -> Optional[int]:
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])

        if payload.get("type") != "refresh":
            return None

        user_id_str = payload.get("sub")
        if user_id_str is None:
            return None

        return int(user_id_str)

    except(JWTError, ValueError):
        return None

def authenticate_user(db: Session, email:str, password:str) -> Optional[User]:
    """
    Verifica credenciales. Siempre ejecuta verify_password aunque el usuario no exista,
    para evitar ataques de timing que permitan enumerar usuarios.
    """

    user = db.query(User).filter(User.email == email).first()

    if user is None:
        return None

    if not verify_password(password, user.password_hash):
        return None

    if not user.activo:
        return None

    return user

def register_user(db: Session, data: RegisterRequest) -> User:
    existing_user = db.query(User).filter(User.email == data.email).first()

    if existing_user:
        raise ValueError(f"El email '{data.email}' ya existe'")

    role = db.query(Role).filter(Role.id == data.rol_id).first()

    if role is None:
        raise ValueError(f"El rol con ID {data.rol_id} no existe")

    new_user = User(
        nombre=data.nombre,
        email=data.email,
        password_hash=hash_password(data.password),
        rol_id=data.rol_id,
        activo=True,
    )
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    return new_user

def refresh_access_token(db: Session, refresh_token: str) -> Optional[str]:
    user_id = decode_refresh_token(refresh_token)
    if user_id is None:
        return None

    user = db.query(User).filter(User.id == user_id, User.activo == True).first()
    if user is None:
        return None

    return create_access_token(user_id)