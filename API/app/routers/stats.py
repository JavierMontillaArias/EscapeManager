from datetime import date
from fastapi import APIRouter, Depends, Query, Request
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import require_manager
from app.limiter import limiter
from app.models.user import User
from app.schemas.stats import EscapeRateItem, HintsAvgItem, OccupancyItem, RankingItem, SummaryResponse
from app.services import stats_service

router = APIRouter(prefix="/stats", tags=["Estadísticas"])


def date_range_params(fecha_inicio: date | None = Query(default=None), fecha_fin: date | None = Query(default=None)):
    return fecha_inicio, fecha_fin


# INC-06: estos dos endpoints no son consumidos por la App — la App deriva escapeRate
# y hintsAvg del resultado de GET /stats/ranking para evitar llamadas adicionales.
# Se mantienen expuestos en Swagger para uso externo o futuras integraciones.
@router.get("/escape-rate", response_model=list[EscapeRateItem])
@limiter.limit("30/minute")
def escape_rate(request: Request, dates: tuple = Depends(date_range_params), db: Session = Depends(get_db), _: User = Depends(require_manager)):
    return stats_service.get_escape_rate(db, *dates)


@router.get("/hints-avg", response_model=list[HintsAvgItem])
@limiter.limit("30/minute")
def hints_avg(request: Request, dates: tuple = Depends(date_range_params), db: Session = Depends(get_db), _: User = Depends(require_manager)):
    return stats_service.get_hints_avg(db, *dates)


@router.get("/occupancy", response_model=list[OccupancyItem])
@limiter.limit("30/minute")
def occupancy(request: Request, dates: tuple = Depends(date_range_params), db: Session = Depends(get_db), _: User = Depends(require_manager)):
    return stats_service.get_occupancy(db, *dates)


@router.get("/ranking", response_model=list[RankingItem])
@limiter.limit("30/minute")
def ranking(request: Request, dates: tuple = Depends(date_range_params), db: Session = Depends(get_db), _: User = Depends(require_manager)):
    return stats_service.get_ranking(db, *dates)


@router.get("/summary", response_model=SummaryResponse)
@limiter.limit("30/minute")
def summary(request: Request, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    return stats_service.get_summary(db)
