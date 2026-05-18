import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)


def test_login_correcto():
    response = client.post("/auth/login", json={
        "email": "admin@escape.com",
        "password": "adminpass123"
    })
    assert response.status_code == 200
    assert "access_token" in response.json()


def test_login_password_incorrecta():
    response = client.post("/auth/login", json={
        "email": "admin@escape.com",
        "password": "wrongpassword"
    })
    assert response.status_code == 401


def test_login_email_inexistente():
    response = client.post("/auth/login", json={
        "email": "noexiste@escape.com",
        "password": "adminpass123"
    })
    assert response.status_code == 401


def test_me_sin_token():
    response = client.get("/auth/me")
    assert response.status_code == 401


def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"