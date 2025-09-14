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
  
- **SLF4J**, **Zipkin**, **Micrometer**, **Spring Boot Actuator** – Logging and distributed tracing.
  
- **Finnhub API** – 3rd Party API that provides stock data and financial news.

---

## System Processing Flow

### Flow 1 – Client Starts Tracking a Symbol

![FLow 1 - Sequence Diagram](https://res.cloudinary.com/dw3x8orox/image/upload/v1753614747/flow1_im1wpv.png)

### Flow 2 – Client Switches to a New Symbol

![Flow 2 - Sequence Diagram](https://res.cloudinary.com/dw3x8orox/image/upload/v1753614747/flow2_owhwwb.png)

---

## APIs and WebSocket

- `GET /company-news`: Return a list of company news for a given stock symbol

  - **Query Parameters**:
    - `symbol`: Stock's symbol. Ex: AAPL (APPLE)


- `/ws` and destination `/app/trackingSymbol`

    ```json
    {
      "currentSymbol": "AAPL",
      "newSymbol": "MSFT"
    }
    ```
  
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

- The server will be exposed through port 1511.

### Build project
```

mvn clean package

```

### Run with Docker Compose
```

docker compose up --build -d

```

### Frontend (Optional)

- A simple frontend file stock-tracker.html is included in the project. You can open it in a browser to quickly test and visualize the system for demonstration purposes.

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

## Demonstration

![Home Page](https://res.cloudinary.com/dw3x8orox/image/upload/v1757674709/Screenshot_2025-09-12_at_17.57.04_shwm0l.png)

---

## License

MIT License

Copyright (c) 2025 BaoDo

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

