from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user, require_manager
from app.limiter import limiter
from app.models.user import User
from app.schemas.auth import LoginRequest, TokenResponse, RefreshRequest, RegisterRequest, MeResponse
from app.schemas.user import UserResponse
from app.services import auth_service

router = APIRouter(prefix="/auth", tags=["Autenticación"])


# PR-1: rate limit en registro para evitar creación masiva de usuarios por un Manager malicioso
@router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
@limiter.limit("10/minute")
def register(request: Request, data: RegisterRequest, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    """Solo Manager puede registrar usuarios.
    Nota: este endpoint devuelve UserResponse (schemas/user.py) con rol anidado como objeto,
    mientras que GET /auth/me devuelve MeResponse (schemas/auth.py) con rol_nombre como string.
    Son schemas distintos por diseño; la App no usa /register actualmente."""
    try:
        return auth_service.register_user(db, data)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


# A11: máximo 5 intentos de login por minuto por IP
@router.post("/login", response_model=TokenResponse)
@limiter.limit("5/minute")
def login(request: Request, data: LoginRequest, db: Session = Depends(get_db)):
    user = auth_service.authenticate_user(db, data.email, data.password)
    if user is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Credenciales incorrectas",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return TokenResponse(
        access_token=auth_service.create_access_token(user.id),
        refresh_token=auth_service.create_refresh_token(user.id),
        token_type="bearer",
        user=MeResponse.model_validate(user),
    )


# S-01: Rate limit en refresh para evitar fuerza bruta sobre refresh tokens.
@router.post("/refresh", response_model=TokenResponse)
@limiter.limit("10/minute")
def refresh(request: Request, data: RefreshRequest, db: Session = Depends(get_db)):
    """
    Genera un nuevo access_token y rota el refresh_token.
    Cada uso del refresh_token emite uno nuevo, invalidando el anterior.
    """
    tokens = auth_service.refresh_access_token(db, data.refresh_token)
    if tokens is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Refresh token inválido o expirado",
        )
    new_access, new_refresh = tokens
    return TokenResponse(
        access_token=new_access,
        refresh_token=new_refresh,
        token_type="bearer",
    )


# SEC-03: logout requiere access token válido para evitar que terceros revoquen
# tokens ajenos si conocieran su valor.
@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
@limiter.limit("20/minute")
def logout(
    request: Request,
    data: RefreshRequest,
    current_user: User = Depends(get_current_user),
):
    """
    Revoca el refresh token del cliente autenticado.
    Solo se revoca si el token pertenece al usuario que hace la petición.
    El cliente debe eliminar también sus tokens localmente.
    """
    if data.refresh_token:
        # Verificar que el refresh token pertenece al usuario autenticado
        user_id = auth_service.decode_refresh_token(data.refresh_token)
        if user_id == current_user.id:
            auth_service.revoke_refresh_token(data.refresh_token)


@router.get("/me", response_model=MeResponse)
def me(current_user: User = Depends(get_current_user)):
    return current_user
