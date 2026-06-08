#!/bin/bash

LIMIT=120
COUNT=0

git status --short

echo "Starting commits from real changed/untracked files..."

while IFS= read -r file; do
  if [ "$COUNT" -ge "$LIMIT" ]; then
    break
  fi

  # берем путь файла из git status
  path="${file:3}"

  # если файл существует, добавляем и коммитим
  if [ -e "$path" ]; then
    git add "$path"

    git commit -m "Update ${path}" || true

    COUNT=$((COUNT + 1))
    echo "Commit $COUNT: $path"
  fi
done < <(git status --short)

echo "Created $COUNT commits."
