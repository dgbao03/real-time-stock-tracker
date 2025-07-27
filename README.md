# Real-Time Stock Price Tracker

## Project Overview

- A backend system designed to track stock prices and related news in real time. 

- It leverages WebSocket communication to deliver live stock price updates and integrates Redis caching to reduce external API calls.

- The backend also handles unsubscribe events by cleaning up unused caches and closing external data streams when no clients are subscribed — ensuring efficient resource usage and always-fresh data for new clients.

- The system is built with Spring Boot and packaged using Docker for deployment. Logging and tracing are enabled using Micrometer, Zipkin, and Spring Boot Actuator.

---

## Technical Stack

- **Java** + **Spring Boot** – REST API & WebSocket system development.
  
- **Redis** – Caching stock prices to minimize API calls.
  
- **Docker** – Containerizing the application for deployment phrase.
  
- **Zipkin**, **Micrometer**, **Spring Boot Actuator** – Logging and distributed tracing.
  
- **Finnhub API** – 3rd Party API that provides stock data and financial news.

---

## System Processing Flow

### Flow 1 – Client Starts Tracking a Symbol

**1. User selects a stock symbol to track (e.g., `META`):**

- The frontend:
  - Establishes a WebSocket connection with the backend.
    
  - Subscribes to the topic of symbol `META` through WS connection.

- The backend:
  - First checks Redis cache to see if the latest price for `META` is already stored.
    
    - If the price is found in cache, it is returned immediately to the client.
      
    - If the price is not found, the backend sends a REST API request to Finnhub to retrieve the latest price, then stores it in Redis and returns it to the client.
      
  - This caching mechanism ensures that if multiple clients are tracking the same symbol, new clients can reuse the cached data without triggering redundant API calls.
    
  - Since at least one client is subscribed, the backend maintains a WebSocket connection with Finnhub for symbol `META` to receive real-time updates.
    
    - Whenever an update is received, the backend updates the Redis cache and broadcasts the new price to all subscribed clients — keeping the cached data fresh and accurate for everyone.

**2. When Finnhub sends a price update for `META`:**

- The backend:
  - Updates the Redis cache with the new price.
    
  - Broadcasts the updated price to all clients subscribed to symbol `META`.

### Flow 2 – Client Switches to a New Symbol

**1. User selects a different symbol to track (e.g., `AAPL`):**

- The frontend:
  - Unsubscribes from the previous symbol (`META`) via WebSocket.
    
  - Initiates a new tracking flow for symbol `AAPL` (same as Flow 1).

- The backend:
  - Checks how many clients are still subscribed to `META`.
  
    - If no clients remain:
      - Removes the cached price of `META`.
        
      - This ensures that if a new client starts tracking `META` in the future, the system will fetch the latest stock price directly from the external API (Finnhub) instead of using potentially outdated cached data.
        
      - Since there are no clients subscribed to `META`, the backend also stops receiving real-time updates from Finnhub for this symbol — meaning the cached price would no longer be updated. Removing the cache prevents serving stale data to future clients.
  
    - If clients are still subscribed:
      - Keeps the cache and the WebSocket connection to Finnhub active, ensuring continued real-time updates and cache freshness.

  - Proceeds to handle tracking for symbol `AAPL` using the same steps as in Flow 1.

 ---

## Setup

- Before running the application, create a `.env` file in the root directory of the project with the following content:

```

FINNHUB_API_KEY = your_finnhub_api_key_here

REDIS_HOST = redis
REDIS_PORT = 6379
CACHE_TYPE = redis

```

---

## Running The Application

### Build project
```

mvn clean package

```

### Run with Docker Compose
```

docker compose up --build -d

```

---

## Observability
### Logging
- The application uses a file-based logging mechanism that automatically rotates logs daily. Logs are written to a designated `logs/` directory with filenames based on the current date.

### Tracing (Distributed Tracing)

- Each step in the system is automatically tagged with:
  - **Trace ID** – A unique ID for the entire request lifecycle
    
  - **Span ID** – A unique ID for each processing step within that trace
    
- Access the Zipkin UI at:
```

http://localhost:9411

```

---

## License

MIT License

Copyright (c) 2025 BaoDo

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

