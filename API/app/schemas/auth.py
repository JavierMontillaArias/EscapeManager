from pydantic import BaseModel, EmailStr, ConfigDict, Field

class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length = 1)

class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str

class RefreshRequest(BaseModel):
    refresh_token: str

class RegisterRequest(BaseModel):
    nombre: str = Field(min_length = 2, max_length = 100)
    email: EmailStr
    password: str = Field(min_length = 8)
    rol_id: int = Field(ge = 1)

class MeResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    nombre: str
    email: str
    rol_id: int
    activo: bool