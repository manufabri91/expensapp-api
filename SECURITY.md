# Security maintenance

The dependency and image security workflow runs on pull requests, pushes to `develop`/`main`, weekly, and on manual dispatch. It fails on high/critical findings or scanner errors and retains JSON scan reports, an inventory manifest, dependency tree, and runtime image SBOM for 30 days. The dependency job inventories the resolved application/test classpath and plugin classpaths actually loaded by `clean verify`, dependency inventory generation and `liquibase:help`. Trivy scans those selected JARs with `rootfs`; an empty or incomplete inventory fails. Raw Maven debug output is filtered in memory and never retained because it can contain credentials. Optional parent plugins and unused cached versions are excluded. The image job scans the actual rebuilt runtime image. Dependabot targets `develop` for Maven, Docker and GitHub Actions updates.

No vulnerabilities are suppressed by default. Any future exception needs the advisory, evidence explaining exposure, an owner and an expiry date; an unknown finding is not resolved. Configure the security jobs as required checks in GitHub branch protection. Scheduled workflows start running only when this file and its workflow are on the repository's default branch; retain GitHub Actions failure notifications for the maintainer account.

## Release checks

Before release, verify PostgreSQL startup/migrations, user ownership checks, Firebase token rejection/refresh, public health checks, CORS and JSON compatibility in staging. Record the deployed commit, image digest and JVM patch. Retain the previous artifact and a database backup; a rollback to an affected version reopens remediation. Production deployments and Firebase-backed end-to-end authentication require the configured environments and are not proven by unit tests or a successful image build.

## Config Server rollout

The API reads configuration from `configserver.manuelfabri.com`. Its client dependency does not update the external server.

On 30 October 2026, the default-branch POM in [manufabri91/config-server](https://github.com/manufabri91/config-server/blob/HEAD/pom.xml) declared Spring Boot `2.3.1.RELEASE`, Spring Cloud `Hoxton.SR6`, and Java `11`. This is source evidence, not an inventory of the running deployment. The repository owner is `manufabri91`; deployment responsibility and the actual server revision/runtime must be confirmed with that owner.

The companion upgrade in the Config Server repository targets the same supported Boot/Cloud versions, preserves ciphertext passthrough and protects crypto management endpoints. Deploy and verify that service separately; changing this API client alone does not patch it. Existing encrypted values are covered by an API compatibility fixture generated with the previous Cloud/Security libraries. Confirm real environment loading and encryption/decryption with non-production configuration before promotion; never print secret plaintext or credentials in scan artifacts.
