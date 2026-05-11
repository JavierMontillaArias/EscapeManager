from pydantic import BaseModel, Field
from datetime import date


class EscapeRateItem(BaseModel):
    sala_id: int
    sala_nombre: str
    total_partidas: int
    partidas_escapadas: int
    tasa_escape: float = Field(description="Porcentaje entre 0.0 y 100.0")


class HintsAvgItem(BaseModel):
    sala_id: int
    sala_nombre: str
    total_partidas: int
    promedio_pistas: float


class OccupancyItem(BaseModel):
    franja: str
    total_reservas: int
    porcentaje: float


class RankingItem(BaseModel):
    posicion: int
    sala_id: int
    sala_nombre: str
    tasa_escape: float
    promedio_pistas: float
    total_partidas: int
    score: float


class SummaryResponse(BaseModel):
    total_reservas: int
    total_partidas: int
    porcentaje_escape_global: float
    sala_mas_popular: str | None
    reservas_hoy: int
    partidas_en_curso: int