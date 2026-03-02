#!/bin/bash
export NVM_DIR="$HOME/.nvm"
source "$NVM_DIR/nvm.sh"
export PORT=4001
cd /opt/earthworm
pnpm dev:serve
