#!/bin/bash

# Script to check RabbitMQ connection and queues

echo "=== Checking RabbitMQ Connection ==="

# Check if RabbitMQ container is running
if docker ps | grep -q rabbitmq-local; then
    echo "✅ RabbitMQ container is running"
else
    echo "❌ RabbitMQ container is not running"
    exit 1
fi

# Check RabbitMQ management interface
echo "Checking RabbitMQ management interface..."
if curl -s -u guest:guest http://localhost:15672/api/overview > /dev/null; then
    echo "✅ RabbitMQ management interface is accessible"
else
    echo "❌ RabbitMQ management interface is not accessible"
    exit 1
fi

# Check if search queues exist
echo "Checking search reindex queues..."
QUEUES=$(curl -s -u guest:guest http://localhost:15672/api/queues | jq -r '.[].name' 2>/dev/null)

if echo "$QUEUES" | grep -q "search.reindex.queue"; then
    echo "✅ search.reindex.queue exists"
else
    echo "⚠️  search.reindex.queue does not exist (will be created when app starts)"
fi

if echo "$QUEUES" | grep -q "search.reindex.batch.queue"; then
    echo "✅ search.reindex.batch.queue exists"
else
    echo "⚠️  search.reindex.batch.queue does not exist (will be created when app starts)"
fi

echo ""
echo "=== RabbitMQ Status ==="
echo "Management UI: http://localhost:15672"
echo "Username: guest"
echo "Password: guest"
echo ""
echo "To view queues: http://localhost:15672/#/queues"