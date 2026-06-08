#!/usr/bin/env bash
set -euo pipefail
URL="${1:-http://localhost:3000/api/network/overview}"
REQUESTS="${REQUESTS:-50}"
echo "Testing $URL with $REQUESTS requests"
TOTAL=0
OK=0
for i in $(seq 1 "$REQUESTS"); do
  START=$(date +%s%3N)
  CODE=$(curl -s -o /dev/null -w "%{http_code}" "$URL" || true)
  END=$(date +%s%3N)
  MS=$((END-START))
  TOTAL=$((TOTAL+MS))
  [ "$CODE" = "200" ] && OK=$((OK+1))
  printf "Request %02d | status=%s | %sms\n" "$i" "$CODE" "$MS"
done
AVG=$((TOTAL/REQUESTS))
echo "Successful responses: $OK/$REQUESTS"
echo "Average response time: ${AVG}ms"
