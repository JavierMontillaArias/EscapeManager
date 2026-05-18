"""add_index_booking_estado

Revision ID: a1b2c3d4e5f6
Revises: 545c6bad25c4
Create Date: 2026-05-17 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'a1b2c3d4e5f6'
down_revision: Union[str, None] = '545c6bad25c4'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # PERF-03: índice en Booking.estado para acelerar queries de estadísticas
    op.create_index(op.f('ix_bookings_estado'), 'bookings', ['estado'], unique=False)


def downgrade() -> None:
    op.drop_index(op.f('ix_bookings_estado'), table_name='bookings')
