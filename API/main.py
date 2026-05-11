import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.security import HTTPBearer

from app.config import settings
from app.routers import auth, users, rooms, bookings, qr, games, incidents, stats

logging.basicConfig(
    level=logging.INFO if settings.ENVIRONMENT == "production" else logging.DEBUG,
    format="%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("EscapeManager API arrancando en modo '%s'", settings.ENVIRONMENT)
    yield
    logger.info("EscapeManager API apagándose")


app = FastAPI(
    title="EscapeManager API",
    description="API REST para gestión operativa de escape rooms con MySQL.",
    version="1.0.0",
    lifespan=lifespan,
    swagger_ui_parameters={"persistAuthorization": True},
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"] if settings.ENVIRONMENT == "development" else [],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    logger.exception("Error no controlado en %s %s", request.method, request.url)
    detail = str(exc) if settings.ENVIRONMENT == "development" else "Error interno del servidor"
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={"detail": detail},
    )


app.include_router(auth.router)
app.include_router(users.router)
app.include_router(rooms.router)
app.include_router(bookings.router)
app.include_router(qr.router)
app.include_router(games.router)
app.include_router(incidents.router)
app.include_router(stats.router)


@app.get("/health", tags=["Sistema"], include_in_schema=False)
def health_check():
    return {"status": "ok", "environment": settings.ENVIRONMENT}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=settings.ENVIRONMENT == "development",
    )