[![Build with Maven](https://github.com/openmrs/openmrs-module-stockmanagement/actions/workflows/build.yml/badge.svg)](https://github.com/openmrs/openmrs-module-stockmanagement/actions/workflows/build.yml)

Stock Management Module
==========================

> OpenMRS backend module for stock management of medical supplies at health facilities.

This module stores stock items, batches, stock operations and stock rules, and exposes them over REST. The O3 frontend that uses it lives in [openmrs-esm-stock-management-app](https://github.com/openmrs/openmrs-esm-stock-management-app).

It is inspired by the [OpenHMIS Inventory Module](https://github.com/banda-health/openmrs-module-openhmis.inventory).

## What it does

- **Stock items** linked to a drug or a concept, with packaging units and batch numbers that carry expiry dates.
- **Stock operations**. Nine operation types are created by Liquibase: Opening Stock, Receipt, Requisition, Stock Issue, Transfer Out, Return, Adjustment, Stock Take and Disposal. An operation moves between the statuses `NEW`, `SUBMITTED`, `DISPATCHED`, `RETURNED`, `REJECTED`, `CANCELLED` and `COMPLETED` through actions such as submit, approve, dispatch and complete.
- **Stock sources and parties**. A party is either an OpenMRS location or a stock source such as a supplier. Operations move stock between parties.
- **Stock rules** that alert or email users when the quantity of an item at a location is at or below a threshold.
- **Reports**, generated in the background as batch jobs and downloaded when they finish.
- **Bulk import** of stock items from a CSV file.
- **User role scopes**, which limit a user's stock privileges to given locations and operation types.

## Requirements

The versions below come from the root `pom.xml` of each maintained branch, which is the source of truth.

| Branch     | Version in `pom.xml`                  | OpenMRS Platform | REST Web Services | Java                                                 |
| ---------- | ------------------------------------- | ---------------- | ----------------- | ---------------------------------------------------- |
| `master`   | 4.0.0-SNAPSHOT                        | 2.8.0            | 3.0.0             | Compiled for Java 8. CI also builds on 11, 17 and 21 |
| `3.x`      | 3.1.0-SNAPSHOT (latest release 3.0.0) | 2.8.0            | 3.0.0             | Compiled for Java 8                                  |
| `core-3.x` | 4.0.0-SNAPSHOT                        | 3.0.0-SNAPSHOT   | 5.0.0-SNAPSHOT    | 21. CI also builds on 25                             |

`3.x` takes fixes for 3.x releases. GitHub Actions builds `master` and `core-3.x` and publishes their snapshots. It doesn't build `3.x` or the older branches:

- `2.x` (latest release 2.0.3) builds against Platform 2.4.3 and REST Web Services 2.32.0 with Java 8.
- `1.X` (latest release 1.4.4) builds against Platform 2.3.1 with Java 8. It also requires 2.x Reference Application modules such as App Framework, UI Framework and Core Apps.

[`omod/src/main/resources/config.xml`](omod/src/main/resources/config.xml) sets what the module needs at runtime:

- **OpenMRS version**: 2.8.0 on `master` and `3.x`, which is the Platform version they build against, and 3.0.0 on `core-3.x`.
- **Required modules**: [REST Web Services](https://github.com/openmrs/openmrs-module-webservices.rest), at the version the branch builds against or later.
- **Aware of**: Legacy UI.

The Liquibase changelog has database-specific changesets for MySQL and PostgreSQL. See [Locations on MariaDB](#locations-on-mariadb) if you run MariaDB.

## Installation

Releases are published to the [OpenMRS Maven repository](https://openmrs.jfrog.io/artifactory/modules/org/openmrs/module/stockmanagement-omod/) as `org.openmrs.module:stockmanagement-omod`. The [O3 reference application](https://github.com/openmrs/openmrs-distro-referenceapplication) already includes the module, through this line in its `distro.properties`:

```properties
omod.stockmanagement=${stockmanagement.version}
```

To install a module file by hand, [build it](#building), put the `.omod` in the `modules` folder of the OpenMRS application data directory and restart OpenMRS. REST Web Services has to be installed as well. Liquibase creates the `stockmgmt_*` tables when the module first starts.

## Configuration

### What the module creates and what you set up

On start the module creates its tables, the nine stock operation types, its privileges and one scheduled task. It also creates a party for every location and fills the location tree it uses to resolve user role scopes.

Up to 3.0.0 the module also created the three location tags below and tagged matching locations, created a Main Store and a Main Pharmacy location where none existed, and seeded the email templates, two more scheduled tasks and default values for ten global properties. `master` and `3.x` no longer do any of this since [#63](https://github.com/openmrs/openmrs-module-stockmanagement/pull/63), so an implementation has to supply them:

- **Location tags** named `Main Store`, `Main Pharmacy` and `Dispensary`. The module looks these up by name. Locations with any of the three tags hold stock, and locations tagged `Main Pharmacy` or `Dispensary` are dispensing locations. Each operation type also accepts only locations with certain tags. For example a Receipt can only go into a `Main Store` location, and a Stock Issue goes from `Main Store` to `Main Pharmacy` or `Dispensary`. `GET /ws/rest/v1/stockmanagement/stockoperationtype?v=default` lists these rules as `stockOperationTypeLocationScopes`.
- **Email templates** named `STOCK_MGMT_OPERATION_ACTION`, `STOCK_MGMT_QTY_BELOW_THRESHOLD` and `STOCK_MGMT_BATCH_EXPIRING`. No email is sent for a template that is missing. The `%CENTER_NAME%` placeholder in the templates comes from the global property `ugandaemr.healthCenterName`, which this module doesn't declare. When it's blank, the module uses the name of the top-level location with the most child locations.
- **Scheduled tasks** for stock rules and batch expiry. See [Scheduled tasks](#scheduled-tasks).
- **Feature-specific global properties** for notifications and reporting. See [Global properties](#global-properties).

Email notifications also require SMTP configuration and valid recipient email addresses. Set the OpenMRS global property `mail.smtp.host` (or `mail.smtp_host`) and the port, authentication and TLS settings required by your mail server. If no SMTP host is configured, the module skips email notifications.

### Privileges

The module declares 17 privileges in [`config.xml`](omod/src/main/resources/config.xml), and OpenMRS creates them when the module starts. `App: stockmanagement.*` privileges allow viewing, for example `App: stockmanagement.stockItems`. Most `Task: stockmanagement.*` privileges allow changes, for example `Task: stockmanagement.stockoperations.approve`. The same names are constants in [`Privileges.java`](api/src/main/java/org/openmrs/module/stockmanagement/api/Privileges.java), and most of the service methods in [`StockManagementService.java`](api/src/main/java/org/openmrs/module/stockmanagement/api/StockManagementService.java) are annotated with the privilege they need.

The module does not create roles. Assign the privileges to roles of your own. A user who requests reports also needs core's `Manage Scheduler` privilege, because queuing a report looks up the report task through the scheduler.

### User role scopes

To list, view, save or act on stock operations, holding a privilege through a role is not enough. The user also needs a user role scope. Scopes also filter the lists of stock on hand, item transactions, stock rules and generated reports, so a user without one, even a superuser, gets those lists empty. Stock on hand looked up for dispensing is the exception for users who hold `Task: stockmanagement.stockItems.dispense.qty`. Nobody can create or change their own scope, so another account has to give an administrator their first one. A scope gives a user one of their roles at chosen locations, optionally including child locations, and for chosen stock operation types. A scope is either permanent or active between two dates, and can be disabled.

When the module checks a stock privilege at a location, it looks for an enabled, active scope for that user that covers the location and the operation type, and then checks that the scope's role has the privilege. `GET /ws/rest/v1/stockmanagement/session` returns the scoped privileges of the logged-in user.

Scopes are managed through the `userrolescope` REST resource, which the frontend uses.

### Locations on MariaDB

The module keeps two tables in step with the OpenMRS `location` table: `stockmgmt_location_tree`, which holds every ancestor and descendant pair, and `stockmgmt_party`, which holds a party per location. Database triggers on `location` do this on MySQL and PostgreSQL.

The MySQL trigger changesets are marked as `mysql` only, and on a MariaDB 10.11 database running 3.0.0 Liquibase skipped them. Without the triggers, the tables are only brought up to date when the module starts. After adding or moving a location on MariaDB, restart the module. `POST /ws/rest/v1/stockmanagement/locationtreesynchronize` rebuilds the location tree without a restart for users with the `Manage Locations` privilege, but it does not create the party for a new location.

### Global properties

The module declares 30 global properties, all prefixed with `stockmanagement.`. [`config.xml`](omod/src/main/resources/config.xml) has the full list with a description of each. These are the ones you are most likely to change:

| Property                                                          | Default | Description                                                                                                             |
| ----------------------------------------------------------------- | ------- | ----------------------------------------------------------------------------------------------------------------------- |
| `stockmanagement.negativeStockBalanceAllowed`                     | `false` | Allow negative stock balances                                                                                           |
| `stockmanagement.allowStockIssueWithoutRequisition`               | `false` | Allow a stock issue that isn't linked to a requisition                                                                  |
| `stockmanagement.excessReceivedItemThreshold`                     | `10`    | Percentage by which the items acknowledged as received may exceed the items issued or returned                          |
| `stockmanagement.stockItemsMaxUploadFileSize`                     | `2`     | Largest stock item import file, in megabytes                                                                            |
| `stockmanagement.enableStockRuleJob`                              | `true`  | Let the stock rule task evaluate stock balances                                                                         |
| `stockmanagement.enableStockBatchJob`                             | `true`  | Let the batch expiry task send notifications                                                                            |
| `stockmanagement.stockBatchDefaultExpiryNotificationNoticePeriod` | `120`   | Default notice period for expiring batches, in days. The notice period set on a stock item takes priority               |
| `stockmanagement.batchJobExpiryInMinutes`                         | `10080` | Minutes after creation when a batch job expires. Expired jobs are deleted with their report files. Values below 1440 are raised to 1440 |

These properties have no default. Their use on `master` is shown below; prefix each property name with `stockmanagement.`.

| Property | When to configure | Value |
| --- | --- | --- |
| `stockOperationNotificationRole` | Optional: send stock-operation alerts to users in a role. Also configure this role when enabling stock-operation emails. | Name of an existing OpenMRS role, not its UUID |
| `stockOperationNotificationEmail` | Optional: send stock-operation emails to this address and to the role's users. Only works when `stockOperationNotificationRole` names an existing role, and needs the email template and SMTP settings. | A single valid email address |
| `observationDispensingLocationConcept` | Optional: use with `observationDrugConcept` to resolve the fulfilment location from observations in prescription reports. | UUID of the concept used for the dispensing-location observation |
| `observationDrugConcept` | Optional: use with `observationDispensingLocationConcept` to match the drug observation in the same observation group. | UUID of the concept used for the drug observation |
| `stockSourceCodeConceptId` | Not used by the current backend. O3 uses `stockSourceTypeUUID` instead. | Leave unset |
| `stockAdjustmentReasonCodeConceptId` | Not used by the current backend. O3 uses `stockAdjustmentReasonUUID` and `stockTakeReasonUUID` instead. | Leave unset |
| `packagingUnitsConceptId` | Not used by the current backend. O3 uses `packingUnitsUUID` instead. | Leave unset |
| `stockItemCategoryConceptId` | Not used by the current backend. O3 uses `stockItemCategoryUUID` instead. | Leave unset |
| `printLogoText` | Not used by the current backend. | Leave unset |
| `applicationRootUrl` | Not used by the current backend. | Leave unset |

Stock operation alerts and emails are only sent for the operation types and actions that have notifications turned on. Liquibase turns them on for submit, approve and complete on Receipt and Transfer Out operations only, and they can't be changed over REST.

The observation properties accept concept UUIDs, not numeric concept IDs. Leave them blank if your implementation does not record those observations; a configured UUID that does not resolve to a concept causes the report to fail. Releases up to 3.0.0 set both, and an upgrade keeps those values, so clear them on an upgraded installation unless they match your concepts.

The O3 concept options above are frontend configuration, defined in its [`config-schema.ts`](https://github.com/openmrs/openmrs-esm-stock-management-app/blob/main/src/config-schema.ts). Set them to the UUIDs of concepts whose coded answers supply the corresponding dropdown choices. Setting the backend global properties does not configure those O3 choices.

### Scheduled tasks

Liquibase registers one task, which starts with OpenMRS:

- **Asynchronous Report Generation And Batch Jobs** (`org.openmrs.module.stockmanagement.api.jobs.AsyncTasksBatchJob`) runs every 60 seconds and generates queued reports. It stops itself when the queue stays empty and is started again when a report is queued. Report files are written to `stockmgmt/batchjob` in the application data directory.

Two more task classes ship with the module. Up to 3.0.0 the module registered them with a daily interval, but not to start with OpenMRS, so they only ran once someone started them in the scheduler. `master` and `3.x` no longer register them, so register them yourself if you want the notifications, for example on the Manage Scheduler page of the Legacy UI, and tick Start on startup so they run again after a restart. A database that ran 3.0.0 or earlier still has them as Stock Rules Evaluation Job and Stock Batch Expiry Job, so edit those instead of adding new ones. The two classes are:

- `org.openmrs.module.stockmanagement.api.jobs.StockRuleEvaluationJob` checks stock rules and notifies users when a quantity is at or below its threshold. It does nothing while `stockmanagement.enableStockRuleJob` is `false`.
- `org.openmrs.module.stockmanagement.api.jobs.StockBatchExpiryJob` notifies users about batches that are about to expire. It does nothing while `stockmanagement.enableStockBatchJob` is `false`. It notifies users who have an active user role scope with the `Inventory Manager` or `Inventory Clerk` role at the batch's location or at a Main Store location. When neither role exists, it uses every role that has `Task: stockmanagement.stockoperations.mutate` instead.

## REST API

Resources are served under `/ws/rest/v1/stockmanagement/`, for example `GET /openmrs/ws/rest/v1/stockmanagement/stockoperationtype`. They are built on the REST Web Services module.

The resource classes are in [`omod/src/main/java/org/openmrs/module/stockmanagement/web/resource`](omod/src/main/java/org/openmrs/module/stockmanagement/web/resource). The resource name is in the `@Resource` annotation of each class:

- **Stock items**: `stockitem`, `stockitempackaginguom`, `stockitemreference`, `stockbatch`, `stockrule`
- **Inventory**: `stockiteminventory`, `stockitemtransaction`
- **Stock operations**: `stockoperation`, `stockoperationitem`, `stockoperationaction`, `stockoperationactionlineitem`, `stockoperationlink`, `stockoperationitemcost`, `stockoperationbatchnumbers`, `stockoperationtype`, `stockoperationtypelocationscope`
- **Sources and locations**: `stocksource`, `party`, `location`
- **Dispensing**: `dispenserequest`, `orderitem`
- **Access**: `userrolescope` with the sub-resources `locations` and `operationtypes`, and `privilegescope`
- **Reports**: `report`, `batchjob`, `batchjobowner`

Five endpoints are plain controllers in [`web/controller`](omod/src/main/java/org/openmrs/module/stockmanagement/web/controller):

| Endpoint                       | Purpose                                                                    |
| ------------------------------ | -------------------------------------------------------------------------- |
| `GET session`                  | The scoped stock privileges of the logged-in user                          |
| `POST stockitemimport`         | Upload a CSV file of stock items. `GET` returns the errors from an upload  |
| `GET batchjobartifact?id=`     | Download the output of a finished batch job, such as a report              |
| `POST locationtreesynchronize` | Rebuild the location tree                                                  |
| `POST indexrebuild`            | Does nothing and returns `true`                                            |

## Development

### Building

You need Maven and a JDK that matches the branch: Java 8, 11, 17 or 21 for `master` and `3.x`, Java 21 or later for `core-3.x`.

```bash
mvn clean package
```

This compiles the module, runs the tests and writes the module file to `omod/target/stockmanagement-<version>.omod`.

On `master` the build also formats the Java code with Spotless and puts the OpenMRS licence header on source and resource files, replacing any other header, using the settings in the [OpenMRS module parent POM](https://github.com/openmrs/openmrs-contrib-maven-parent-module). Commit the changes it makes. In CI the build checks the formatting and headers instead of applying them, and fails if any file differs. The Spotless version depends on the JDK, and CI checks with each JDK it builds on. If a line formats differently on Java 8 than on Java 17 or later, rewrite it so both agree, for example by moving a nested call into a local variable.

To skip the tests:

```bash
mvn clean package -DskipTests
```

### Running tests

```bash
mvn test
```

The tests are in `api/src/test` and `omod/src/test`. On `master` they use JUnit 5 and the `jupiter` base test classes from OpenMRS core.

### Deploying to a local backend

The quickest way to try a build is against the [O3 reference application](https://github.com/openmrs/openmrs-distro-referenceapplication) running in Docker. Its backend container deletes `/openmrs/data/modules` on every start and refills it from `/openmrs/distribution/openmrs_modules`, so replace the module file in the distribution folder:

```bash
docker exec <backend-container> ls /openmrs/distribution/openmrs_modules | grep stockmanagement
```

```bash
docker cp omod/target/stockmanagement-<version>.omod <backend-container>:/openmrs/distribution/openmrs_modules/<name of the file listed above>
```

```bash
docker restart <backend-container>
```

Copy over the existing file name so that only one version of the module is in the folder. Recreating the container brings back the version that the image shipped with.

On a standalone server, replace the `.omod` in the `modules` folder of the application data directory and restart OpenMRS.

## Contributing

- Tickets are in the [O3 project](https://openmrs.atlassian.net/browse/O3) in Jira.
- Branch off `master`, or off `core-3.x` for changes that only apply to Platform 3. A fix that both lines need gets a pull request against each branch, and one against `3.x` too if it should ship in a 3.x release.
- Pull requests are usually squash-merged, so the PR title becomes the commit message. Title them `(type) O3-1234: Summary of the change`, for example `(fix) O3-6201: Reject stock actions without an operation UUID`.
- Add or update tests for the behaviour you change. CI runs the build on every pull request to `master` and `core-3.x`.
- Report security issues privately. See [SECURITY.md](SECURITY.md).

## Releasing

Releases are made with the [Release workflow](.github/workflows/release.yml), which is started by hand from the Actions tab with three inputs: the release version, the next development version and the branch to release from. It runs the Maven release plugin, which tags the release as `stockmanagement-<version>` and publishes it to the OpenMRS Maven repository, and then creates a GitHub release with generated notes.

To release from `3.x` or `core-3.x`, run the workflow from that branch and set the branch input to the same branch. The copy of the workflow on `core-3.x` sets Java 21, and the copies on `master` and `3.x` set Java 8.
