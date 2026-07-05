import uuid
from datetime import datetime

from sqlalchemy import Boolean, DateTime, Integer, String, Text, func
from sqlalchemy.dialects.postgresql import ARRAY, UUID
from sqlalchemy.orm import Mapped, mapped_column

from db import Base


class Track(Base):
    """Agrupa skills em trilhas curriculares para o perfil público."""

    __tablename__ = "tracks"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    slug: Mapped[str] = mapped_column(String(60), unique=True, nullable=False)  # "algebra"
    name: Mapped[str] = mapped_column(String(120), nullable=False)              # "Trilha de Álgebra"
    description: Mapped[str] = mapped_column(Text, default="")
    skill_tags: Mapped[list[str]] = mapped_column(ARRAY(String), nullable=False)
    display_order: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
