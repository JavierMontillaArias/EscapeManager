from datetime import timedelta, timezone, datetime
from typing import Optional

from jose import jwt, JWTError
from passlib.context import CryptContext
from sqlalchemy.orm import Session

from app.config import settings
from app.models import User, Role
from app.schemas import RegisterRequest

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# S-07: Generado en tiempo de carga del módulo para que no sea predecible ni hardcodeado.
DUMMY_HASH = pwd_context.hash("dummy_password_for_timing_mitigation")

# S-02: Blacklist en memoria de refresh tokens revocados.
# Se pierde al reiniciar el servidor (suficiente para TFG sin Redis).
_revoked_tokens: set[str] = set()


def revoke_refresh_token(token: str) -> None:
    """Añade un refresh token a la blacklist en memoria."""
    _revoked_tokens.add(token)


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
        expires_delta=timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES),
        token_type="access",
    )


def create_refresh_token(user_id: int) -> str:
    return _create_token(
        data={"sub": str(user_id)},
        expires_delta=timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS),
        token_type="refresh",
    )


def decode_refresh_token(token: str) -> Optional[int]:
    # S-02: Rechazar tokens explícitamente revocados (logout).
    if token in _revoked_tokens:
        return None

    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])

        if payload.get("type") != "refresh":
            return None

        user_id_str = payload.get("sub")
        if user_id_str is None:
            return None

        return int(user_id_str)

    except (JWTError, ValueError):
        return None


def authenticate_user(db: Session, email: str, password: str) -> Optional[User]:
    """
    Verifica credenciales. Siempre ejecuta verify_password antes de comprobar activo,
    para evitar timing side-channel que permita distinguir usuarios inactivos de inexistentes.
    """
    user = db.query(User).filter(User.email == email).first()
    hash_to_verify = user.password_hash if user else DUMMY_HASH
    password_ok = pwd_context.verify(password, hash_to_verify)
    if not user or not password_ok or not user.activo:
        return None
    return user


ASSIGNABLE_ROLES = {"Game Master"}


def register_user(db: Session, data: RegisterRequest) -> User:
    existing_user = db.query(User).filter(User.email == data.email).first()

    if existing_user:
        raise ValueError(f"El email '{data.email}' ya existe")

    role = db.query(Role).filter(Role.id == data.rol_id).first()

    if role is None:
        raise ValueError(f"El rol con ID {data.rol_id} no existe")

    if role.nombre not in ASSIGNABLE_ROLES:
        raise ValueError(f"No se puede asignar el rol '{role.nombre}'")

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


def refresh_access_token(db: Session, refresh_token: str) -> Optional[tuple[str, str]]:
    """
    Verifica el refresh_token y devuelve (nuevo_access_token, nuevo_refresh_token).
    El refresh token rota en cada uso para limitar la ventana de ataque.
    """
    user_id = decode_refresh_token(refresh_token)
    if user_id is None:
        return None

    user = db.query(User).filter(User.id == user_id, User.activo == True).first()
    if user is None:
        return None

    # Rotar: revocar el token usado y emitir uno nuevo.
    revoke_refresh_token(refresh_token)
    return create_access_token(user_id), create_refresh_token(user_id)
