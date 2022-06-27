# aims-bulk-scheduler
Spring Boot Microservice scheduler for new AIMS bulk match solution. It follows the following sequence of events:

1. The app subscribes to the scheduler topic in GCP. When a new bulk matching job completes the Scheduler app is notified by a PubSub message.
2. The app polls the BigQuery dataset to determine when all results have moved from the buffer to the result table. The polling frequency is configurable.
3. A PubSub message is raised to the export topic.
4. A Cloud function triggers on the export topic and exports the contents of the BigQuery results table to a new GCS bucket.

