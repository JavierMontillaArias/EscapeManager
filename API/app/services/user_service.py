from typing import Optional

from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.orm import Session

from app.models.user import User
from app.schemas.user import UserUpdate
from app.services.auth_service import hash_password

# CAL-01: whitelist explícita de campos actualizables vía PATCH/PUT /users/{id}.
# Evita que si alguien añadiera 'password' a UserUpdate en el futuro, se aplique
# como texto plano al password_hash sin pasar por hash_password().
ALLOWED_USER_UPDATE_FIELDS = {"nombre", "email", "activo"}


def get_user_by_id(db: Session, user_id: int) -> Optional[User]:
    return db.query(User).filter(User.id == user_id).first()


def get_all_users(db: Session, skip: int = 0, limit: int = 200) -> list[User]:
    return db.query(User).order_by(User.id).offset(skip).limit(limit).all()


def update_user(db: Session, user_id: int, data: UserUpdate) -> Optional[User]:
    user = db.query(User).filter(User.id == user_id).first()
    if user is None:
        return None

    update_data = data.model_dump(exclude_unset=True)

    # CAL-01: filtrar campos no permitidos antes de aplicar al ORM
    update_data = {k: v for k, v in update_data.items() if k in ALLOWED_USER_UPDATE_FIELDS}

    if "email" in update_data and update_data["email"] != user.email:
        email_taken = (
            db.query(User)
            .filter(User.email == update_data["email"], User.id != user_id)
            .first()
        )
        if email_taken:
            raise ValueError(f"El email '{update_data['email']}' ya está en uso")

    # SEC-01: misma protección que deactivate_user — no desactivar al último Manager activo
    if update_data.get("activo") is False and user.activo:
        if user.rol.nombre == "Manager":
            active_managers = (
                db.query(User)
                .join(User.rol)
                .filter(User.activo == True, User.rol.has(nombre="Manager"))
                .count()
            )
            if active_managers <= 1:
                raise ValueError("No se puede desactivar al único Manager activo")

    for field, value in update_data.items():
        setattr(user, field, value)

    try:
        db.commit()
        db.refresh(user)
    except SQLAlchemyError:
        db.rollback()
        raise

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
    try:
        db.commit()
        db.refresh(user)
    except SQLAlchemyError:
        db.rollback()
        raise

    return user
