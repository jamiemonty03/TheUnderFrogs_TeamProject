import os
import time
import logging

from load_fact_trades import TradeDataETL

logger = logging.getLogger(__name__)


def run_once():
    try:
        TradeDataETL().run()
    except Exception as e:
        logger.error(f"ETL run failed: {e}")


def main():
    interval = int(os.getenv('ETL_INTERVAL_SECONDS', '300'))
    logger.info(f"Starting FACT_TRADES ETL scheduler, running every {interval}s")
    while True:
        run_once()
        time.sleep(interval)


if __name__ == "__main__":
    main()
