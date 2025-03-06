import newrelic.agent
import os
import sys
from django.core.wsgi import get_wsgi_application


sys.pycache_prefix = '{PYCACHE_PREFIX}'

try:
    with open('/srv/wsgiapps/configs/newrelic.ini') as f:
        pass
    newrelic.agent.initialize('/srv/wsgiapps/configs/newrelic.ini', '{ENVIRONMENT}')
except IOError:
    # No newrelic reporting
    pass
finally:
    os.environ.setdefault('DJANGO_SETTINGS_MODULE', '{SETTINGS_FILE}')
    application = get_wsgi_application()
