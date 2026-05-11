from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.dependencies import get_current_user, require_manager
from app.models.user import User
from app.schemas.incident import IncidentCreate, IncidentResponse, IncidentResolveResponse
from app.services import incident_service

router = APIRouter(prefix="/incidents", tags=["Incidencias"])


@router.get("", response_model=list[IncidentResponse])
def list_incidents(db: Session = Depends(get_db), current_user: User = Depends(get_current_user)):
    return incident_service.get_incidents(db, current_user)


@router.post("", response_model=IncidentResponse, status_code=status.HTTP_201_CREATED)
def create_incident(data: IncidentCreate, db: Session = Depends(get_db),current_user: User = Depends(get_current_user)):
    try:
        return incident_service.create_incident(db, data, current_user)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


@router.patch("/{incident_id}/resolve", response_model=IncidentResolveResponse)
def resolve_incident(incident_id: int, db: Session = Depends(get_db), _: User = Depends(require_manager)):
    try:
        incident = incident_service.resolve_incident(db, incident_id)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    if incident is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Incidencia no encontrada")
    return IncidentResolveResponse(
        id=incident.id,
        resuelta=incident.resuelta,
        fecha_resolucion=incident.fecha_resolucion,
    )