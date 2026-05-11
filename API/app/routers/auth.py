from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user, require_manager
from app.models.user import User
from app.schemas.auth import LoginRequest, TokenResponse, RefreshRequest, RegisterRequest, MeResponse
from app.schemas.user import UserResponse
from app.services import auth_service

router = APIRouter(prefix="/auth", tags=["Autenticación"])


@router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
def register(data: RegisterRequest, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    """Solo Manager puede registrar usuarios."""
    try:
        return auth_service.register_user(db, data)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


@router.post("/login", response_model=TokenResponse)
def login(data: LoginRequest, db: Session = Depends(get_db)):
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
    )


@router.post("/refresh", response_model=TokenResponse)
def refresh(data: RefreshRequest, db: Session = Depends(get_db)):
    new_token = auth_service.refresh_access_token(db, data.refresh_token)
    if new_token is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Refresh token inválido o expirado",
        )
    return TokenResponse(
        access_token=new_token,
        refresh_token=data.refresh_token,
        token_type="bearer",
    )


@router.get("/me", response_model=MeResponse)
def me(current_user: User = Depends(get_current_user)):
    return current_user