from typing import Optional
from sqlalchemy.orm import Session

from app.models.user import User
from app.schemas.user import UserCreate, UserUpdate
from app.services.auth_service import hash_password


def get_user_by_id(db: Session, user_id:int) -> Optional[User]:
    return db.query(User).filter(User.id == user_id).first()

def get_all_users(db: Session) -> list[User]:
    return db.query(User).order_by(User.id).all()

def create_user(db: Session, data: UserCreate) -> User:
    existing = db.query(User).filter(User.email == data.email).first()

    if existing:
        raise ValueError(f"El email '{data.email}' ya está registrado")

    user = User(
        nombre=data.nombre,
        email=data.email,
        password_hash=hash_password(data.password),
        rol_id=data.rol_id,
        activo=True,
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user

def update_user(db: Session, user_id: int, data: UserUpdate) -> Optional[User]:
    user = db.query(User).filter(User.id == user_id).first()
    if user is None:
        return None

    if data.email is not None and data.email != user.email:
        email_taken = (
            db.query(User)
            .filter(User.email == data.email, User.id != user_id)
            .first()
        )
        if email_taken:
            raise ValueError(f"El email '{data.email}' ya está en uso")
        user.email = data.email

    if data.nombre is not None:
        user.nombre = data.nombre

    if data.activo is not None:
        user.activo = data.activo

    db.commit()
    db.refresh(user)
    return user

def deactivate_user(db: Session, user_id: int) -> Optional[User]:
    user = db.query(User).filter(User.id == user_id).first()
    if user is None:
        return None

    if user.rol.nombre == "Manager" and user.activo:
        active_managers = (
            db.query(User)
            .join(User.rol)
            .filter(User.activo == True, User.rol.has(nombre="Manager"))
            .count()
        )
        if active_managers <= 1:
            raise ValueError("No se puede desactivar al único Manager activo")

    user.activo = False
    db.commit()
    db.refresh(user)
    return user