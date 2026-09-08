MIGRATION_DIR := src/main/resources/db/migration

migration:
	@if [ -z "$(name)" ]; then \
		echo "Error: Migration name is required. Usage: make migration name=<migration_name>"; \
		exit 1; \
	fi; \
	timestamp=$$(date +'%Y_%m_%d_%H%M%S'); \
	filename="V$$timestamp""__$(name).sql"; \
	touch $(MIGRATION_DIR)/$$filename; \
	echo "Created migration file: $$filename"


