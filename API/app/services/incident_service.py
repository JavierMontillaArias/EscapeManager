from datetime import datetime, timezone
from typing import Optional

from sqlalchemy.orm import Session

from app.models.incident import Incident
from app.models.room import Room
from app.models.user import User
from app.schemas.incident import IncidentCreate


def get_incidents(db: Session, current_user: User) -> list[Incident]:
    query = db.query(Incident)
    if current_user.rol.nombre == "Game Master":
        query = query.filter(Incident.usuario_id == current_user.id)
    return query.order_by(Incident.fecha.desc()).all()


def create_incident(db: Session, data: IncidentCreate, current_user: User) -> Incident:
    room = db.query(Room).filter(Room.id == data.sala_id).first()
    if room is None:
        raise ValueError(f"La sala con ID {data.sala_id} no existe")

    incident = Incident(
        sala_id=data.sala_id,
        usuario_id=current_user.id,
        descripcion=data.descripcion,
        resuelta=False,
        fecha_resolucion=None,
    )
    db.add(incident)
    db.commit()
    db.refresh(incident)
    return incident


def resolve_incident(db: Session, incident_id: int) -> Optional[Incident]:
    incident = db.query(Incident).filter(Incident.id == incident_id).first()
    if incident is None:
        return None

    if incident.resuelta:
        raise ValueError("Esta incidencia ya está marcada como resuelta")

    incident.resuelta = True
    incident.fecha_resolucion = datetime.now()

    db.commit()
    db.refresh(incident)
    return incident