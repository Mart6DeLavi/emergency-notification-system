# SENSA — Kubernetes (k3s) demo slice

Дополнительный вариант развёртывания: **k3s на том же сервере**, рядом с
Docker Compose. Compose остаётся основным способом поднять весь стек; k3s
показывает оркестрацию для stateless-части.

Что попадает в кластер:

| Компонент | В кластере | В Docker Compose |
|-----------|:----------:|:----------------:|
| `rescue-panel` | ✅ | |
| `api-gateway` | ✅ | |
| `load-balancer-service` | ✅ | |
| `rate-limiter-service` | ✅ | |
| `authentication-service` | ✅ | |
| `emergency-situation-request-service` | ✅ | |
| `template-service` | ✅ | |
| `user-data-management-service` | | ✅ |
| `notification-service` | | ✅ |
| `filesystem-service` | | ✅ |
| `message-delivery-service` | | ✅ |
| PostgreSQL / MongoDB / Redis / Kafka / RabbitMQ | | ✅ |
| Prometheus / Loki / Grafana | | ✅ |

Сервисы из Compose доступны кластеру через публичный IP узла
(`external.host`), который задаётся в `values.yaml`.

## 1. Установка k3s

```bash
curl -sfL https://get.k3s.io | sh -s - --write-kubeconfig-mode 644
sudo kubectl get nodes
```

Traefik входит в k3s. Переключи его на фиксированные NodePort, чтобы Caddy
мог проксировать на него:

```bash
sudo kubectl apply -f k8s/k3s/traefik-nodeport.yaml
sudo kubectl -n kube-system rollout restart deploy/traefik
```

## 2. Подготовка хоста (Docker Compose)

Инфраструктура и «хостовые» сервисы должны быть доступны из подов. На сервере
в `/opt/sensa/.env`:

```dotenv
SENSA_BIND_IP=0.0.0.0
KAFKA_ADVERTISED_HOST=169.58.188.177
K8S_DOMAIN=k8s.sensanotify.com
```

> `SENSA_BIND_IP=0.0.0.0` публикует порты инфраструктуры на узле. Снаружи их
> закрывает firewall провайдера (`thesis-fw`: только 22/80/443). Если хочешь
> строже — привяжи к IP docker-моста и проверь доступность из пода.

Пересоздай затронутые контейнеры:

```bash
cd /opt/sensa
docker compose up -d postgres mongo redis kafka \
  user-data-management-service notification-service filesystem-service
```

Caddy получит новый маршрут `k8s.<домен>` → `host.docker.internal:30080`
(блок уже есть в `docker/Caddyfile`). DNS-запись `k8s` покрывается wildcard
`*.sensanotify.com`.

## 3. Сборка и импорт образов

k3s использует containerd, а не Docker. Образы, собранные Docker/Jenkins,
нужно импортировать:

```bash
cd /opt/sensa
docker compose build \
  api-gateway load-balancer-service rate-limiter-service \
  authentication-service emergency-situation-request-service \
  template-service rescue-panel

for img in api-gateway load-balancer-service rate-limiter-service \
           authentication-service emergency-situation-request-service \
           template-service rescue-panel; do
  docker save "sensa/$img:latest" | sudo k3s ctr images import -
done
```

`values.yaml` использует `imagePullPolicy: Never` и `imageRegistry: sensa`,
поэтому имена образов должны совпадать с `sensa/<service>:latest`.

## 4. Установка чарта

```bash
cd /opt/sensa
helm upgrade --install sensa k8s/helm/sensa \
  --namespace sensa --create-namespace \
  --set ingress.host=k8s.sensanotify.com \
  --set external.host=169.58.188.177 \
  --set secrets.jwtSecret="$(grep '^JWT_USER_SECRET=' .env | cut -d= -f2-)" \
  --set secrets.postgresUsername="$(grep '^POSTGRES_USERNAME=' .env | cut -d= -f2-)" \
  --set secrets.postgresPassword="$(grep '^POSTGRES_PASSWORD=' .env | cut -d= -f2-)"
```

## 5. Проверка

```bash
sudo kubectl -n sensa get pods,svc,ingress,hpa
sudo kubectl -n sensa logs deploy/api-gateway
curl -I https://k8s.sensanotify.com
```

Ожидаемо:

- панель и API доступны на `https://k8s.sensanotify.com` (TLS от Caddy);
- HPA для `api-gateway` виден в `kubectl get hpa`;
- поды проходят readiness/liveness-пробы.

## 6. Опционально: cert-manager

По умолчанию TLS терминирует Caddy на хосте. Если нужен TLS внутри кластера:

```bash
helm repo add jetstack https://charts.jetstack.io
helm repo update
helm upgrade --install cert-manager jetstack/cert-manager \
  -n cert-manager --create-namespace --set crds.enabled=true
```

Затем в `values.yaml`: `certManager.enabled=true`, `ingress.tlsSecret=sensa-tls`
и аннотация `cert-manager.io/cluster-issuer: letsencrypt-prod`.

## 7. Удаление

```bash
helm uninstall sensa -n sensa
sudo kubectl delete namespace sensa
```

## Ограничения

- Один узел k3s; stateful-инфраструктура остаётся в Docker.
- HPA работает по CPU (metrics-server входит в k3s).
- Kafka доступна подам по `external.host:29092` (listener `PLAINTEXT_HOST`).
- `SENSA_BIND_IP=0.0.0.0` требует работающего firewall провайдера.
