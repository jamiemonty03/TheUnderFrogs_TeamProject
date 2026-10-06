import json
import os
import threading
from collections import deque
from typing import Dict, List, Optional
from datetime import datetime
from confluent_kafka import Consumer, KafkaError
from dotenv import load_dotenv

load_dotenv()

# Global state for live data
live_prices: Dict[str, dict] = {}  # {symbol: {price, timestamp, currency}}
live_trades: deque = deque(maxlen=100)  # Keep last 100 trade events
kafka_available = False
consumer_threads = []

# Kafka configuration
KAFKA_BROKER = os.getenv('KAFKA_BROKER', 'kafka:9092')
MARKET_DATA_TOPIC = 'market-data'
TRADE_EVENTS_TOPIC = 'trade-events'


def parse_event_envelope(message_value: str) -> Optional[dict]:
    """Parse S7-1 event envelope from Kafka message."""
    try:
        return json.loads(message_value)
    except json.JSONDecodeError:
        print(f"Failed to parse event envelope: {message_value}")
        return None


def market_data_consumer_thread():
    """Background thread consuming market-data topic."""
    global kafka_available, live_prices
    
    conf = {
        'bootstrap.servers': KAFKA_BROKER,
        'group.id': 'dashboard-market-data',
        'auto.offset.reset': 'earliest',
        'enable.auto.commit': True,
        'session.timeout.ms': 6000,
    }
    
    try:
        consumer = Consumer(conf)
        consumer.subscribe([MARKET_DATA_TOPIC])
        kafka_available = True
        print(f"✓ Connected to market-data topic on {KAFKA_BROKER}")
        
        while True:
            msg = consumer.poll(timeout=1.0)
            
            if msg is None:
                continue
            
            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    continue
                else:
                    print(f"Consumer error: {msg.error()}")
                    kafka_available = False
                    break
            
            try:
                event = parse_event_envelope(msg.value().decode('utf-8'))
                if event:
                    # Support both formats: new format with 'payload' and legacy format with 'body'
                    data = event.get('payload', event.get('body', {}))
                    if 'symbol' in data and 'price' in data:
                        live_prices[data['symbol']] = {
                            'price': float(data['price']),
                            'timestamp': data.get('quoteTime', datetime.utcnow().isoformat()),
                            'currency': data.get('currency', 'USD'),
                        }
            except Exception as e:
                print(f"Error processing market-data: {e}")
        
        consumer.close()
    except Exception as e:
        print(f"Market data consumer error: {e}")
        kafka_available = False


def trade_events_consumer_thread():
    """Background thread consuming trade-events topic."""
    global kafka_available, live_trades
    
    conf = {
        'bootstrap.servers': KAFKA_BROKER,
        'group.id': 'dashboard-trade-events',
        'auto.offset.reset': 'earliest',
        'enable.auto.commit': True,
        'session.timeout.ms': 6000,
    }
    
    try:
        consumer = Consumer(conf)
        consumer.subscribe([TRADE_EVENTS_TOPIC])
        kafka_available = True
        print(f"✓ Connected to trade-events topic on {KAFKA_BROKER}")
        
        while True:
            msg = consumer.poll(timeout=1.0)
            
            if msg is None:
                continue
            
            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    continue
                else:
                    print(f"Consumer error: {msg.error()}")
                    kafka_available = False
                    break
            
            try:
                event = parse_event_envelope(msg.value().decode('utf-8'))
                if event:
                    # Support both formats: new format with 'payload' and legacy format with 'body'
                    data = event.get('payload', event.get('body', {}))
                    # Extract relevant fields: event_type, orderId, symbol, quantity, fillPrice, status, etc.
                    trade_event = {
                        'event_type': event.get('eventType', data.get('type', 'UNKNOWN')),
                        'order_id': data.get('orderId', data.get('id', 'N/A')),
                        'symbol': data.get('symbol', 'N/A'),
                        'quantity': data.get('quantity', 0),
                        'price': data.get('fillPrice', data.get('price', 0)),  # fillPrice from OrderOutcomePayload
                        'status': data.get('status', 'UNKNOWN'),
                        'timestamp': data.get('timestamp', datetime.utcnow().isoformat()),
                        'account_id': data.get('accountId', 'N/A'),
                    }
                    live_trades.append(trade_event)
            except Exception as e:
                print(f"Error processing trade-events: {e}")
        
        consumer.close()
    except Exception as e:
        print(f"Trade events consumer error: {e}")
        kafka_available = False


def start_kafka_consumers():
    """Start background Kafka consumer threads."""
    global consumer_threads, kafka_available
    
    # Market data consumer thread
    t1 = threading.Thread(target=market_data_consumer_thread, daemon=True)
    t1.start()
    consumer_threads.append(t1)
    
    # Trade events consumer thread
    t2 = threading.Thread(target=trade_events_consumer_thread, daemon=True)
    t2.start()
    consumer_threads.append(t2)
    
    print("✓ Kafka consumer threads started")


def get_live_prices() -> Dict[str, dict]:
    """Get current live prices from market-data topic."""
    return dict(live_prices) if kafka_available else {}


def get_live_trades() -> List[dict]:
    """Get recent trade events from trade-events topic."""
    return list(live_trades) if kafka_available else []


def is_kafka_available() -> bool:
    """Check if Kafka consumers are connected."""
    return kafka_available
