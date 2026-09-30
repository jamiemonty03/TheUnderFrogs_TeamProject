import os
import sys
from unittest.mock import patch

etl_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'etl'))
if etl_dir not in sys.path:
    sys.path.insert(0, etl_dir)

import run_scheduler


class TestRunScheduler:
    def test_run_once_creates_new_etl_each_run(self):
        with patch('run_scheduler.TradeDataETL') as mock_etl:
            run_scheduler.run_once()
            run_scheduler.run_once()

            assert mock_etl.call_count == 2
            assert mock_etl.return_value.run.call_count == 2

    def test_run_once_swallows_etl_failure(self):
        with patch('run_scheduler.TradeDataETL') as mock_etl:
            mock_etl.return_value.run.side_effect = Exception("db down")

            run_scheduler.run_once()

            mock_etl.return_value.run.assert_called_once()
