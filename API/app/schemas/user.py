from pydantic import BaseModel, EmailStr, ConfigDict, Field
from datetime import datetime

class UserBase(BaseModel):
    nombre: str = Field(min_length = 2, max_length = 100)
    email: EmailStr

class UserCreate(UserBase):
    password: str = Field(min_length = 8)
    rol_id: int = Field(ge = 1)

class UserUpdate(UserBase):
    nombre: str | None = Field(default=None, min_length = 2, max_length = 100)
    email: EmailStr | None = None
    activo: bool | None = None

class RolResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    nombre: str


class UserResponse(UserBase):
    model_config = ConfigDict(from_attributes=True)

    id: int
    rol_id: int
    rol: RolResponse
    activo: bool
    created_at: datetime