# Matchly — Kubernetes (kustomize)

> **STARTER manifests.** These are intended to be *reviewed and hardened before
> any production use*. They favor readability over completeness: placeholder
> secrets in plaintext, a single shared Postgres, no NetworkPolicies/PDBs/TLS,
> and ingress annotations for one controller only. Treat this as a scaffold.

## Layout

```
infra/k8s/
  base/                     # 10 app components + shared config (no backing infra)
    namespace.yaml          # namespace: matchly
    configmap.yaml          # shared non-secret env (Kafka, Redis, service URLs)
    secret.yaml             # PLACEHOLDER secret (JWT, DB creds, LLM keys)
    <component>.yaml        # Deployment + ClusterIP Service per component
    ingress.yaml            # /api + /actuator -> gateway, / -> frontend
    hpa.yaml                # HPAs: api-gateway, matching-service, ai-service
    kustomization.yaml
    infra-dev/              # DEV-ONLY Postgres/Redis/Kafka (referenced by dev overlay)
  overlays/
    dev/                    # 1 replica, small resources, in-cluster infra, :dev tags
    prod/                   # 2-3 replicas, larger resources, managed AWS infra, pinned tags
```

The 10 components and their container ports: api-gateway(8080), auth-service(8081),
candidate-service(8082), job-service(8083), application-service(8084),
matching-service(8085), interview-service(8086), analytics-service(8087),
ai-service(8000), frontend(80).

Health probes: Spring services -> `/actuator/health`, ai-service ->
`/internal/health`, frontend -> `/`.

## Prerequisites

- A Kubernetes cluster and `kubectl` (>= 1.27) with a current context.
- `kustomize` v5 (bundled in `kubectl`, so `kubectl apply -k` works).
- An **ingress controller** (the example ingress targets `ingressClassName:
  nginx`). On EKS, switch to the AWS Load Balancer Controller.
- **metrics-server** installed (required for the HorizontalPodAutoscalers).
- Container images pushed to the registry referenced by the image names
  (`matchly/<component>:<tag>` — replace `matchly/` with your real registry,
  e.g. an ECR URL from `infra/terraform`).

## What you MUST replace before applying

1. **`base/secret.yaml`** — every value is a placeholder. Replace `JWT_SECRET`,
   Postgres creds, and (optionally) LLM keys. In a real cluster manage this with
   a SealedSecret / External Secrets Operator / AWS Secrets Manager, not
   plaintext `stringData`.
2. **Image registry + tags** — overlays set `:dev` / `:v1.0.0`. Point them at
   your registry and pin a real release tag for prod.
3. **prod ConfigMap hosts** — `overlays/prod/kustomization.yaml` has
   `REPLACE_WITH_MSK_BOOTSTRAP_BROKERS` and
   `REPLACE_WITH_ELASTICACHE_PRIMARY_ENDPOINT`. Point them at the managed AWS
   endpoints (Terraform outputs).
4. **Ingress** — adjust `ingressClassName` and annotations for your controller,
   and add a host + TLS in any real environment.

## Apply

Render to preview (no cluster changes):

```bash
kubectl kustomize infra/k8s/overlays/dev    # or .../prod
```

Dev (single-replica everything + in-cluster Postgres/Redis/Kafka):

```bash
kubectl apply -k infra/k8s/overlays/dev
kubectl -n matchly get pods
```

Prod (2-3 replicas, expects managed AWS backing infra to already exist):

```bash
# Edit overlays/prod first (image tags, ConfigMap endpoints, real secret).
kubectl apply -k infra/k8s/overlays/prod
```

Tear down:

```bash
kubectl delete -k infra/k8s/overlays/dev
```

## Notes / known gaps (intentional, for a starter)

- **Backing infra**: `base/` has no Postgres/Redis/Kafka. The **dev** overlay
  layers in `base/infra-dev/` (single-replica, ephemeral). **prod** assumes
  managed AWS (RDS / ElastiCache / MSK) provisioned via `infra/terraform`.
- DB-per-service is honored on one Postgres instance: the dev init script
  creates `auth_db, candidate_db, ... ai_db` and enables `pgvector` on `ai_db`.
- The ai-service `DATABASE_URL` is composed from the placeholder secret creds
  via `$(VAR)` env substitution.
- Not included (add before production): PodDisruptionBudgets, NetworkPolicies,
  resource quotas, TLS/cert-manager, service mesh / mTLS, structured-log and
  Prometheus scrape annotations.
