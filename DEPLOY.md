# Deploy Configuration - Kafka Metamorphosis

## Quick Setup

Run these commands to configure deployment:

```bash
# 1. Configure credentials (one time only)
source deploy-setup.sh

# 2. Deploy
./deploy.sh
```

## Manual Configuration

If you prefer to configure manually:

```bash
# Export environment variables
export CLOJARS_USERNAME="caioclavico"
export CLOJARS_PASSWORD="your-deploy-token-here"

# Check configuration
lein check
lein test

# Deploy
lein deploy clojars
```

## Important Files

- `project.clj` - Project configuration and dependencies
- `deploy-setup.sh` - Script to configure credentials
- `deploy.sh` - Automated deploy script
- `RELEASE.md` - Complete release guide

## Current Status

- ✅ Project configured for deployment
- ✅ Automated scripts created  
- ✅ Documentation updated
- ✅ Successfully deployed to Clojars

## Next Step

Execute: `source deploy-setup.sh && ./deploy.sh`
