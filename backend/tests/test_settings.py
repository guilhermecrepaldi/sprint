import unittest

from db import Settings


class SettingsTests(unittest.TestCase):
    def test_auto_create_tables_defaults_to_dev_friendly_true(self):
        settings = Settings()
        self.assertTrue(settings.auto_create_tables)

    def test_postgresql_url_is_normalized_to_asyncpg(self):
        settings = Settings(database_url="postgresql://user:pass@localhost:5432/app")
        self.assertEqual(settings.async_database_url, "postgresql+asyncpg://user:pass@localhost:5432/app")


if __name__ == "__main__":
    unittest.main()
