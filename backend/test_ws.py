import asyncio
import websockets

async def run_ws_smoke():
    uri = "ws://localhost:8000/api/telemetry/stream"
    try:
        async with websockets.connect(uri) as websocket:
            print("WS Connect OK")
            await websocket.send('{"x": 100, "y": 200, "t": 12345}')
            response = await websocket.recv()
            print(f"WS Resp: {response}")
    except Exception as e:
        print(f"WS Error: {e}")

if __name__ == "__main__":
    asyncio.run(run_ws_smoke())
