from datetime import date
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import require_manager
from app.models.user import User
from app.schemas.stats import EscapeRateItem, HintsAvgItem, OccupancyItem, RankingItem, SummaryResponse
from app.services import stats_service

router = APIRouter(prefix="/stats", tags=["Estadísticas"])


def date_range_params(fecha_inicio: date | None = Query(default=None), fecha_fin: date | None = Query(default=None)):
    return fecha_inicio, fecha_fin


@router.get("/escape-rate", response_model=list[EscapeRateItem])
def escape_rate(dates: tuple = Depends(date_range_params), db: Session = Depends(get_db), _: User = Depends(require_manager)):
    return stats_service.get_escape_rate(db, *dates)


@router.get("/hints-avg", response_model=list[HintsAvgItem])
def hints_avg(dates: tuple = Depends(date_range_params), db: Session = Depends(get_db), _: User = Depends(require_manager)):
    return stats_service.get_hints_avg(db, *dates)


@router.get("/occupancy", response_model=list[OccupancyItem])
def occupancy(dates: tuple = Depends(date_range_params), db: Session = Depends(get_db), _: User = Depends(require_manager)):
    return stats_service.get_occupancy(db, *dates)


@router.get("/ranking", response_model=list[RankingItem])
def ranking(dates: tuple = Depends(date_range_params), db: Session = Depends(get_db), _: User = Depends(require_manager)):
    return stats_service.get_ranking(db, *dates)


@router.get("/summary", response_model=SummaryResponse)
def summary(db: Session = Depends(get_db), _: User = Depends(require_manager)):
    return stats_service.get_summary(db)