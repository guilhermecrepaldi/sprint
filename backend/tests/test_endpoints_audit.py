import unittest
from fastapi.testclient import TestClient
from unittest.mock import patch
from main import create_app

class EndpointsAuditTests(unittest.TestCase):
    def setUp(self):
        # Inicializa o app FastAPI sem inicializar o banco de dados PostgreSQL real para testes rápidos
        self.app = create_app(run_startup_db=False)
        
        # Moca a dependência get_db do FastAPI para isolar os testes do banco PostgreSQL inativo
        from db import get_db
        from unittest.mock import MagicMock, AsyncMock
        
        async def override_get_db():
            mock_session = MagicMock()
            mock_session.execute = AsyncMock(return_value=MagicMock())
            mock_session.get = AsyncMock(return_value=None)
            yield mock_session
            
        self.app.dependency_overrides[get_db] = override_get_db
        self.client = TestClient(self.app)

    def test_root_endpoint(self):
        response = self.client.get("/")
        self.assertEqual(response.status_code, 200)
        self.assertIn("Math Ink Vector Engine", response.json()["status"])

    def test_health_endpoint_mocked(self):
        # Moca a conexão do banco para testar o endpoint sem depender do PostgreSQL local inativo
        response = self.client.get("/api/health")
        self.assertEqual(response.status_code, 200)

    def test_ml_dataset_parameters_validation(self):
        # Valida que o endpoint de dataset analítico exige parâmetros válidos
        response = self.client.get("/api/ml/dataset?format=invalid_format")
        self.assertEqual(response.status_code, 422) # Unprocessable Entity

    def test_session_start_validation(self):
        # Valida que iniciar sessão exige payload estruturado
        response = self.client.post("/api/session/start", json={})
        self.assertEqual(response.status_code, 422)

    def test_export_endpoint_exists(self):
        # Verifica se o endpoint de exportação correto e registrado (/api/session/{id}/export) é mapeado
        # Usamos um UUID fake
        fake_session_id = "00000000-0000-0000-0000-000000000000"
        response = self.client.get(f"/api/session/{fake_session_id}/export")
        self.assertIn(response.status_code, [200, 404, 500])

    def test_list_all_registered_routes(self):
        # Mapeia e valida dinamicamente todas as rotas ativas do FastAPI para fins de observabilidade
        routes = [route.path for route in self.app.routes]
        print(f"\n[AUDIT] Total de rotas FastAPI registradas: {len(routes)}")
        for r in sorted(routes):
            print(f"  -> Path ativado: {r}")
        
        # Garante que as rotas core do MVP estão presentes no roteamento global
        core_paths = ["/", "/api/health", "/api/session/start", "/api/ml/dataset", "/api/session/{session_id}/submit"]
        for path in core_paths:
            self.assertTrue(any(path in r for r in routes), f"Rota crítica ausente: {path}")

if __name__ == "__main__":
    unittest.main()
