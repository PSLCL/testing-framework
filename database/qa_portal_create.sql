-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema qa_portal
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema qa_portal
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `qa_portal` DEFAULT CHARACTER SET latin1 ;
USE `qa_portal` ;

-- -----------------------------------------------------
-- Table `qa_portal`.`module`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `qa_portal`.`module` (
  `pk_module` INT(11) NOT NULL AUTO_INCREMENT,
  `organization` VARCHAR(100) NOT NULL,
  `name` VARCHAR(256) NOT NULL,
  `version` VARCHAR(45) NOT NULL,
  `status` VARCHAR(45) NOT NULL DEFAULT '',
  `sequence` VARCHAR(45) NOT NULL,
  `attributes` VARCHAR(200) NOT NULL,
  `scheduled_release` DATE NULL DEFAULT NULL,
  `actual_release` DATE NULL DEFAULT NULL,
  `discovered_on` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `missing_count` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`pk_module`),
  UNIQUE INDEX `module_UNIQUE` (`organization` ASC, `name` ASC, `version` ASC, `sequence` ASC, `attributes` ASC))
ENGINE = InnoDB
AUTO_INCREMENT = 2
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `qa_portal`.`content`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `qa_portal`.`content` (
  `pk_content` BINARY(32) NOT NULL,
  `is_generated` TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`pk_content`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `qa_portal`.`artifact`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `qa_portal`.`artifact` (
  `pk_artifact` INT(11) NOT NULL AUTO_INCREMENT,
  `fk_module` INT(11) NOT NULL,
  `fk_content` BINARY(32) NOT NULL,
  `configuration` VARCHAR(45) NOT NULL,
  `name` VARCHAR(256) NOT NULL,
  `mode` INT NULL,
  `merge_source` TINYINT(1) NOT NULL,
  `derived_from_artifact` INT(11) NOT NULL,
  `merged_from_module` INT(11) NULL,
  PRIMARY KEY (`pk_artifact`),
  INDEX `fk_module_idx` (`fk_module` ASC),
  INDEX `fk_artifact_content_idx` (`fk_content` ASC),
  INDEX `name_idx` (`name` ASC),
  INDEX `merged_from_module_idx` (`merged_from_module` ASC),
  CONSTRAINT `fk_module`
    FOREIGN KEY (`fk_module`)
    REFERENCES `qa_portal`.`module` (`pk_module`)
    ON DELETE CASCADE
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_content`
    FOREIGN KEY (`fk_content`)
    REFERENCES `qa_portal`.`content` (`pk_content`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_merged_module`
    FOREIGN KEY (`merged_from_module`)
    REFERENCES `qa_portal`.`module` (`pk_module`)
    ON DELETE CASCADE
    ON UPDATE RESTRICT)
ENGINE = InnoDB
AUTO_INCREMENT = 2
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `qa_portal`.`test_plan`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `qa_portal`.`test_plan` (
  `pk_test_plan` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(256) NULL DEFAULT NULL,
  `description` LONGTEXT NULL DEFAULT NULL,
  PRIMARY KEY (`pk_test_plan`))
ENGINE = InnoDB
AUTO_INCREMENT = 9
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `qa_portal`.`test`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `qa_portal`.`test` (
  `pk_test` INT(11) NOT NULL AUTO_INCREMENT,
  `fk_test_plan` INT(11) NOT NULL,
  `name` VARCHAR(256) NULL DEFAULT NULL,
  `description` LONGTEXT NULL DEFAULT NULL,
  `script` VARCHAR(256) NOT NULL,
  `last_run` DATETIME NULL DEFAULT NULL,
  `last_stdout` LONGTEXT NULL DEFAULT NULL,
  `last_stderr` LONGTEXT NULL DEFAULT NULL,
  PRIMARY KEY (`pk_test`),
  INDEX `fk_test_plan` (`fk_test_plan` ASC),
  CONSTRAINT `test_ibfk_1`
    FOREIGN KEY (`fk_test_plan`)
    REFERENCES `qa_portal`.`test_plan` (`pk_test_plan`)
    ON DELETE CASCADE
    ON UPDATE RESTRICT)
ENGINE = InnoDB
AUTO_INCREMENT = 79
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `qa_portal`.`template`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `qa_portal`.`template` (
  `pk_template` INT(11) NOT NULL AUTO_INCREMENT,
  `hash` BINARY(32) NOT NULL,
  `steps` MEDIUMTEXT NOT NULL,
  `enabled` TINYINT(1) NULL,
  PRIMARY KEY (`pk_template`),
  UNIQUE INDEX `hash_UNIQUE` (`hash` ASC))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `qa_portal`.`run`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `qa_portal`.`run` (
  `pk_run` INT(11) NOT NULL AUTO_INCREMENT,
  `fk_template` INT NOT NULL,
  `artifacts` LONGBLOB NULL DEFAULT NULL,
  `start_time` DATETIME NOT NULL,
  `ready_time` DATETIME NULL DEFAULT NULL,
  `end_time` DATETIME NULL DEFAULT NULL,
  `result` TINYINT(1) NULL DEFAULT NULL,
  `owner` VARCHAR(128) NULL DEFAULT NULL,
  PRIMARY KEY (`pk_run`),
  INDEX `fk_template_run_template1_idx` (`fk_template` ASC),
  CONSTRAINT `fk_run_template1`
    FOREIGN KEY (`fk_template`)
    REFERENCES `qa_portal`.`template` (`pk_template`)
    ON DELETE CASCADE
    ON UPDATE RESTRICT)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `qa_portal`.`described_template`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `qa_portal`.`described_template` (
  `pk_described_template` INT(11) NOT NULL AUTO_INCREMENT,
  `fk_module_set` BINARY(32) NOT NULL,
  `fk_template` INT(11) NOT NULL,
  `description_hash` BINARY(32) NOT NULL,
  `synchronized` TINYINT(1) NULL,
  PRIMARY KEY (`pk_described_template`),
  INDEX `fk_described_template_template1_idx` (`fk_template` ASC),
  UNIQUE INDEX `extended_key` (`fk_module_set` ASC, `fk_template` ASC),
  CONSTRAINT `fk_described_template_template1`
    FOREIGN KEY (`fk_template`)
    REFERENCES `qa_portal`.`template` (`pk_template`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `qa_portal`.`test_instance`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `qa_portal`.`test_instance` (
  `pk_test_instance` INT(11) NOT NULL AUTO_INCREMENT,
  `description` LONGTEXT NULL,
  `fk_test` INT(11) NOT NULL,
  `fk_described_template` INT(11) NOT NULL,
  `fk_run` INT(11) NULL,
  `due_date` DATETIME NULL,
  `phase` INT NOT NULL,
  `synchronized` TINYINT(1) NULL,
  PRIMARY KEY (`pk_test_instance`),
  INDEX `fk_test` (`fk_test` ASC),
  INDEX `fk_test_instance_run1_idx` (`fk_run` ASC),
  INDEX `fk_test_instance_description1_idx` (`fk_described_template` ASC),
  CONSTRAINT `test_instance_ibfk_1`
    FOREIGN KEY (`fk_test`)
    REFERENCES `qa_portal`.`test` (`pk_test`)
    ON DELETE CASCADE
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_test_instance_run1`
    FOREIGN KEY (`fk_run`)
    REFERENCES `qa_portal`.`run` (`pk_run`)
    ON DELETE SET NULL
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_test_instance_described_template1`
    FOREIGN KEY (`fk_described_template`)
    REFERENCES `qa_portal`.`described_template` (`pk_described_template`)
    ON DELETE CASCADE
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `qa_portal`.`run_to_run`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `qa_portal`.`run_to_run` (
  `fk_parent` INT(11) NOT NULL,
  `fk_child` INT(11) NOT NULL,
  INDEX `fk_run_to_run_run1_idx` (`fk_parent` ASC),
  INDEX `fk_run_to_run_run2_idx` (`fk_child` ASC),
  CONSTRAINT `fk_run_to_run_run1`
    FOREIGN KEY (`fk_parent`)
    REFERENCES `qa_portal`.`run` (`pk_run`)
    ON DELETE CASCADE
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_run_to_run_run2`
    FOREIGN KEY (`fk_child`)
    REFERENCES `qa_portal`.`run` (`pk_run`)
    ON DELETE CASCADE
    ON UPDATE RESTRICT)
ENGINE = InnoDB
DEFAULT CHARACTER SET = latin1;


-- -----------------------------------------------------
-- Table `qa_portal`.`dt_line`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `qa_portal`.`dt_line` (
  `pk_dt_line` INT(11) NOT NULL AUTO_INCREMENT,
  `fk_described_template` INT(11) NOT NULL,
  `line` INT NOT NULL,
  `fk_child_dt` INT(11) NULL,
  `description` MEDIUMTEXT NULL,
  PRIMARY KEY (`pk_dt_line`),
  INDEX `fk_dt_line_described_template1_idx` (`fk_described_template` ASC),
  CONSTRAINT `fk_dt_line_described_template1`
    FOREIGN KEY (`fk_described_template`)
    REFERENCES `qa_portal`.`described_template` (`pk_described_template`)
    ON DELETE CASCADE
    ON UPDATE RESTRICT)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `qa_portal`.`dt_to_dt`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `qa_portal`.`dt_to_dt` (
  `fk_parent` INT(11) NOT NULL,
  `fk_child` INT(11) NOT NULL,
  INDEX `fk_dt_to_dt_described_template1_idx` (`fk_parent` ASC),
  INDEX `fk_dt_to_dt_dt_line1_idx` (`fk_child` ASC),
  CONSTRAINT `fk_dt_to_dt_described_template1`
    FOREIGN KEY (`fk_parent`)
    REFERENCES `qa_portal`.`described_template` (`pk_described_template`)
    ON DELETE CASCADE
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_dt_to_dt_dt_line1`
    FOREIGN KEY (`fk_child`)
    REFERENCES `qa_portal`.`dt_line` (`pk_dt_line`)
    ON DELETE CASCADE
    ON UPDATE RESTRICT)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `qa_portal`.`artifact_to_dt_line`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `qa_portal`.`artifact_to_dt_line` (
  `fk_artifact` INT(11) NOT NULL,
  `fk_dt_line` INT(11) NOT NULL,
  `is_primary` TINYINT(1) NULL,
  `reason` VARCHAR(45) NULL,
  INDEX `fk_artifact_to_dt_line_artifact1_idx` (`fk_artifact` ASC),
  INDEX `fk_artifact_to_dt_line_dt_line1_idx` (`fk_dt_line` ASC),
  CONSTRAINT `fk_artifact_to_dt_line_artifact1`
    FOREIGN KEY (`fk_artifact`)
    REFERENCES `qa_portal`.`artifact` (`pk_artifact`)
    ON DELETE CASCADE
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_artifact_to_dt_line_dt_line1`
    FOREIGN KEY (`fk_dt_line`)
    REFERENCES `qa_portal`.`dt_line` (`pk_dt_line`)
    ON DELETE CASCADE
    ON UPDATE RESTRICT)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `qa_portal`.`artifact_provider`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `qa_portal`.`artifact_provider` (
  `pk_provider` INT(11) NOT NULL AUTO_INCREMENT,
  `classname` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`pk_provider`),
  UNIQUE INDEX `pk_provider_UNIQUE` (`pk_provider` ASC),
  UNIQUE INDEX `classname_UNIQUE` (`classname` ASC))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `qa_portal`.`module_to_test_instance`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `qa_portal`.`module_to_test_instance` (
  `fk_module` INT(11) NOT NULL,
  `fk_test_instance` INT(11) NOT NULL,
  UNIQUE INDEX `module_to_test_instance_UNIQUE` (`fk_module` ASC, `fk_test_instance` ASC),
  INDEX `fk_module_to_test_instance_test_instance1_idx` (`fk_test_instance` ASC),
  CONSTRAINT `fk_module_to_test_instance_test_instance1`
    FOREIGN KEY (`fk_test_instance`)
    REFERENCES `qa_portal`.`test_instance` (`pk_test_instance`)
    ON DELETE CASCADE
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_module_to_test_instance_module1`
    FOREIGN KEY (`fk_module`)
    REFERENCES `qa_portal`.`module` (`pk_module`)
    ON DELETE CASCADE
    ON UPDATE RESTRICT)
ENGINE = InnoDB;

USE `qa_portal` ;

-- -----------------------------------------------------
-- procedure add_run
-- -----------------------------------------------------

DELIMITER $$
USE `qa_portal`$$
CREATE PROCEDURE `add_run`( IN hash Char(64) CHARACTER SET utf8, IN result Boolean, IN owner VARCHAR(128), IN started DATETIME, IN ready DATETIME, IN complete DATETIME )
proc_label: BEGIN
	DECLARE template INT(11);
	DECLARE t TIMESTAMP;
	DECLARE pk_run INT(11);
	DECLARE needs_run INT;

	-- Deteremine the primary key of the template referred to by the hash.
	SELECT pk_template INTO template FROM template WHERE hex(template.hash)=hash LIMIT 1;
	IF template IS NULL THEN
		SELECT concat('Hash ', hash, ' not found.');
		CALL raise_error;
	END IF;

	DROP TEMPORARY TABLE IF EXISTS instances;
	CREATE TEMPORARY TABLE instances engine=memory
	SELECT pk_test_instance
		FROM test_instance
		LEFT JOIN described_template ON described_template.pk_described_template = test_instance.fk_described_template
		LEFT JOIN template ON described_template.fk_template = template.pk_template
		WHERE fk_template=template;

	SELECT count(*) INTO needs_run FROM instances;
	IF needs_run = 0 THEN
		LEAVE proc_label;
	END IF;

	IF started IS NULL THEN
		SET started = CURRENT_TIMESTAMP;
	END IF;
    
    IF result IS NULL THEN
		SET complete = NULL;
	END IF;
    
	INSERT INTO run (fk_template, start_time, ready_time, end_time, result, owner) VALUES (template,started,ready,complete,result,owner);
	SET pk_run = LAST_INSERT_ID();

	UPDATE test_instance SET fk_run=pk_run WHERE pk_test_instance IN ( SELECT pk_test_instance FROM instances );

	DROP TEMPORARY TABLE IF EXISTS instances;
	SELECT pk_run;
END$$

DELIMITER ;

-- -----------------------------------------------------
-- procedure get_descriptions_by_version
-- -----------------------------------------------------

DELIMITER $$
USE `qa_portal`$$
CREATE PROCEDURE `get_descriptions_by_version`( IN versions varchar(255) )
BEGIN
	SET @where = '';
	IF NOT versions IS NULL THEN
		SET @where = concat( ' AND pk_version in ( ', versions, ')' );
	END IF;

	DROP TEMPORARY TABLE IF EXISTS test_instances;
	SET @query = concat( 'CREATE TEMPORARY TABLE test_instances engine=memory
	SELECT DISTINCT
		version.pk_version, test_instance.fk_test, test_instance.pk_test_instance, test_instance.fk_run, test_instance.fk_described_template
	FROM
		version
		JOIN artifact on artifact.fk_Version = version.pk_version
		JOIN artifact_to_dt_line ON artifact.pk_artifact = artifact_to_dt_line.fk_artifact
		JOIN dt_line ON artifact_to_dt_line.fk_dt_line = dt_line.pk_dt_line
		JOIN described_template ON dt_line.fk_described_template = described_template.pk_described_template
		JOIN test_instance ON described_template.pk_described_template = test_instance.fk_described_template
	WHERE artifact_to_dt_line.is_primary = 1', @where);

	PREPARE stmt FROM @query;
	EXECUTE stmt;
	DEALLOCATE PREPARE stmt;

	select
		dt_line.*
	from
		test_instances
		join described_template on described_template.pk_described_template = test_instances.fk_described_template
		join dt_line on dt_line.fk_described_template = described_template.pk_described_template
	order by dt_line.fk_described_template, dt_line.line;

	DROP TEMPORARY TABLE IF EXISTS test_instances;
END$$

DELIMITER ;

-- -----------------------------------------------------
-- procedure get_detail_by_version
-- -----------------------------------------------------

DELIMITER $$
USE `qa_portal`$$
CREATE PROCEDURE `get_detail_by_version`( IN versions varchar(255) )
BEGIN
	SET @where = '';
	IF NOT versions IS NULL THEN
		SET @where = concat( ' AND pk_version in ( ', versions, ')' );
	END IF;

	DROP TEMPORARY TABLE IF EXISTS test_instances;
	SET @query = concat( 'CREATE TEMPORARY TABLE test_instances engine=memory
	SELECT DISTINCT
		version.pk_version, test_instance.fk_test, test_instance.pk_test_instance, test_instance.fk_run, test_instance.fk_described_template
	FROM
		version
		JOIN artifact on artifact.fk_Version = version.pk_version
		JOIN artifact_to_dt_line ON artifact.pk_artifact = artifact_to_dt_line.fk_artifact
		JOIN dt_line ON artifact_to_dt_line.fk_dt_line = dt_line.pk_dt_line
		JOIN described_template ON dt_line.fk_described_template = described_template.pk_described_template
		JOIN test_instance ON described_template.pk_described_template = test_instance.fk_described_template
	WHERE artifact_to_dt_line.is_primary = 1', @where);

	PREPARE stmt FROM @query;
	EXECUTE stmt;
	DEALLOCATE PREPARE stmt;

	SELECT 
        component.pk_component, 
		version.pk_version,
        test_plan.pk_test_plan,
        test.pk_test,
        test_instances.pk_test_instance,
        test.name AS test_name,
        run.passed,
        run.start_time,
        run.end_time,
		described_template.pk_described_template
	FROM
		test_instances
		LEFT JOIN run ON run.pk_run = test_instances.fk_run
		JOIN test ON test.pk_test = test_instances.fk_test
		JOIN test_plan ON test_plan.pk_test_plan = test.fk_test_plan
		JOIN version ON version.pk_version = test_instances.pk_version
		JOIN component ON component.pk_component = version.fk_component
		JOIN described_template ON described_template.pk_described_template = test_instances.fk_described_template
     ORDER BY component.name, version.sort_order, test_plan.name;

END$$

DELIMITER ;

-- -----------------------------------------------------
-- procedure get_instance_list
-- -----------------------------------------------------

DELIMITER $$
USE `qa_portal`$$
CREATE PROCEDURE `get_instance_list`( IN test_plan INT(11), IN test INT(11), IN module INT(11), IN module_list VARCHAR(255) )
BEGIN
	DECLARE strLen INT DEFAULT 0;
    DECLARE SubStrLen INT DEFAULT 0;
    DECLARE m VARCHAR(10) DEFAULT "";
    DECLARE module_list_count INT DEFAULT 0;
    
    DECLARE CONTINUE HANDLER FOR 1062
		SET module_list_count = module_list_count - 1;
        
	DROP TEMPORARY TABLE IF EXISTS modules;
	DROP TEMPORARY TABLE IF EXISTS test_instances;
	IF module_list IS NULL THEN
		SET module_list = '';
	END IF;
    
	IF module IS NULL AND module_list = '' THEN
		CREATE TEMPORARY TABLE test_instances (fk_test_instance INT(11) NOT NULL PRIMARY KEY) AS
		SELECT pk_test_instance AS fk_test_instance
		FROM test_instance;
	ELSE
		CREATE TEMPORARY TABLE modules (module INT(11) NOT NULL PRIMARY KEY);
        
        IF NOT module IS NULL THEN
			INSERT INTO modules VALUES ( module );
        END IF;
        
        IF module_list <> '' THEN
			module_loop:
			LOOP
				SET strLen = LENGTH(module_list);
				SET m = SUBSTRING_INDEX(module_list,',',1);
				INSERT INTO modules VALUES (m);
				SET module_list_count = module_list_count + 1;
				SET SubStrLen = LENGTH(m) + 2;
				SET module_list=MID(module_list, SubStrLen, strLen);
				IF module_list = '' THEN
					LEAVE module_loop;
				END IF;
			END LOOP module_loop;
        END IF;
    
		CREATE TEMPORARY TABLE test_instances (fk_test_instance INT(11) NOT NULL PRIMARY KEY) AS
		SELECT PS1.fk_test_instance
		   FROM module_to_test_instance AS PS1, modules AS H1
		  WHERE PS1.fk_module = H1.module
		  GROUP BY PS1.fk_test_instance
		  HAVING COUNT(PS1.fk_module) >= module_list_count;
    
	END IF;
    
    DROP TEMPORARY TABLE IF EXISTS qualified;
    SET @where_word = '';
    SET @and_word = '';
	SET @test_plan_where = '';
	IF NOT test_plan IS NULL THEN
		SET @where_word = ' WHERE ';
		SET @test_plan_where = concat( 'pk_test_plan=', test_plan );
	END IF;

	SET @test_where = '';
    IF NOT test IS NULL THEN
		SET @where_word = ' WHERE ';
        IF @test_plan_where <> '' THEN
			SET @and_word = ' AND ';
		END IF;
		SET @test_where = concat( 'pk_test=', test );
	END IF;
    
	SET @query = concat(
	'CREATE TEMPORARY TABLE qualified (fk_test_instance INT(11) NOT NULL PRIMARY KEY ) AS
	SELECT test_instances.fk_test_instance
	FROM test_instances
	LEFT JOIN test_instance ON test_instance.pk_test_instance = test_instances.fk_test_instance
	LEFT JOIN test ON test.pk_test = test_instance.fk_test
	LEFT JOIN test_plan ON test_plan.pk_test_plan = test.fk_test_plan'
    , @where_word, @test_plan_where, @and_word, @test_where, ';');

	PREPARE stmt FROM @query;
	EXECUTE stmt;
	DEALLOCATE PREPARE stmt;

	# Create a table of all used modules
	DROP TEMPORARY TABLE IF EXISTS sorted_modules;
    SET @row_num = -1; # We desire zero-based index
	CREATE TEMPORARY TABLE sorted_modules (pk_module INT(11) NOT NULL PRIMARY KEY, offset INT,
		organization VARCHAR(100), name VARCHAR(100), version VARCHAR(45), sequence VARCHAR(45), attributes VARCHAR(200)) AS
    SELECT module.pk_module, @row_num := @row_num + 1 AS offset, organization, name, version, sequence, attributes
    FROM 
    module
    LEFT JOIN
		(SELECT DISTINCT module.pk_module FROM qualified
		LEFT JOIN module_to_test_instance ON module_to_test_instance.fk_test_instance = qualified.fk_test_instance
		LEFT JOIN module ON module.pk_module = module_to_test_instance.fk_module) m ON m.pk_module = module.pk_module
    ORDER BY offset, organization, name, attributes, sequence;
    
	# First response, list of test plans, tests, instances, and modules.
	SELECT pk_test_plan, pk_test, pk_test_instance, test_instance.description, GROUP_CONCAT(sorted_modules.offset) AS modules, result, end_time
	FROM qualified
    LEFT JOIN module_to_test_instance ON module_to_test_instance.fk_test_instance = qualified.fk_test_instance
    LEFT JOIN sorted_modules ON sorted_modules.pk_module = module_to_test_instance.fk_module
	LEFT JOIN test_instance ON test_instance.pk_test_instance = qualified.fk_test_instance
	LEFT JOIN test ON test.pk_test = test_instance.fk_test
	LEFT JOIN test_plan ON test_plan.pk_test_plan = test.fk_test_plan
    LEFT JOIN run ON run.pk_run = test_instance.fk_run
    GROUP BY pk_test_instance
    ORDER BY test_plan.name, test.name;
    
    # Second response, test plan information
    SELECT DISTINCT test_plan.pk_test_plan, test_plan.name, test_plan.description
    FROM qualified
	LEFT JOIN test_instance ON test_instance.pk_test_instance = qualified.fk_test_instance
	LEFT JOIN test ON test.pk_test = test_instance.fk_test
	LEFT JOIN test_plan ON test_plan.pk_test_plan = test.fk_test_plan
    ORDER BY test_plan.name;

	# Third response, test information
    SELECT DISTINCT test.pk_test, test.name, test.description
    FROM qualified
 	LEFT JOIN test_instance ON test_instance.pk_test_instance = qualified.fk_test_instance
	LEFT JOIN test ON test.pk_test = test_instance.fk_test
    ORDER BY test.name;
   
    # Four response, module information in sorted order
    SELECT * FROM sorted_modules;
    
 #   SELECT pk_described_template
 #   FROM qualified
 #   LEFT JOIN test_instance ON test_instance.pk_test_instance = qualified.fk_test_instance
 #   LEFT JOIN described_template ON described_template.pk_described_template = test_instance.fk_described_template
 #   LEFT JOIN dt_to_dt ON dt_to_dt.fk_parent = described_template.pk_described_template
 #   LEFT JOIN dt_line ON dt_
#	SELECT pk_test_instance
 #   FROM test_instance;
    
 #   SELECT pk_test
 #   FROM test;
    
    DROP TEMPORARY TABLE IF EXISTS modules;
    DROP TEMPORARY TABLE IF EXISTS test_instances;
    DROP TEMPORARY TABLE IF EXISTS qualified;
    DROP TEMPORARY TABLE IF EXISTS sorted_modules;
END$$

DELIMITER ;

-- -----------------------------------------------------
-- procedure get_resources_by_version
-- -----------------------------------------------------

DELIMITER $$
USE `qa_portal`$$
CREATE PROCEDURE `get_resources_by_version`( IN versions varchar(255) )
BEGIN
	SET @where = '';
	IF NOT versions IS NULL THEN
		SET @where = concat( ' AND pk_version in ( ', versions, ')' );
	END IF;

	DROP TEMPORARY TABLE IF EXISTS test_instances;
	SET @query = concat( 'CREATE TEMPORARY TABLE test_instances engine=memory
	SELECT DISTINCT
		version.pk_version, test_instance.fk_test, test_instance.pk_test_instance, test_instance.fk_run, test_instance.fk_described_template
	FROM
		version
		JOIN artifact on artifact.fk_Version = version.pk_version
		JOIN artifact_to_dt_line ON artifact.pk_artifact = artifact_to_dt_line.fk_artifact
		JOIN dt_line ON artifact_to_dt_line.fk_dt_line = dt_line.pk_dt_line
		JOIN described_template ON dt_line.fk_described_template = described_template.pk_described_template
		JOIN test_instance ON described_template.pk_described_template = test_instance.fk_described_template
	WHERE artifact_to_dt_line.is_primary = 1', @where);

	PREPARE stmt FROM @query;
	EXECUTE stmt;
	DEALLOCATE PREPARE stmt;

	select distinct
		resource.*
	from
		test_instances
		join described_template on described_template.pk_described_template = test_instances.fk_described_template
		join dt_line on dt_line.fk_described_template = described_template.pk_described_template
		join resource on dt_line.fk_resource = resource.pk_resource
	order by dt_line.fk_described_template, dt_line.line;

	DROP TEMPORARY TABLE IF EXISTS test_instances;
END$$

DELIMITER ;

-- -----------------------------------------------------
-- procedure get_summary_by_version
-- -----------------------------------------------------

DELIMITER $$
USE `qa_portal`$$
CREATE PROCEDURE `get_summary_by_version`( IN versions varchar(255) )
BEGIN
	SET @where = '';
	IF NOT versions IS NULL THEN
		SET @where = concat( ' AND pk_version in ( ', versions, ')' );
	END IF;

	DROP TEMPORARY TABLE IF EXISTS test_instances;
	SET @query = concat( 'CREATE TEMPORARY TABLE test_instances engine=memory
	SELECT DISTINCT
		version.pk_version, test_instance.fk_test, test_instance.pk_test_instance, test_instance.fk_run
	FROM
		version
		JOIN artifact on artifact.fk_Version = version.pk_version
		JOIN artifact_to_dt_line ON artifact.pk_artifact = artifact_to_dt_line.fk_artifact
		JOIN dt_line ON artifact_to_dt_line.fk_dt_line = dt_line.pk_dt_line
		JOIN described_template ON dt_line.fk_described_template = described_template.pk_described_template
		JOIN test_instance ON described_template.pk_described_template = test_instance.fk_described_template
	WHERE artifact_to_dt_line.is_primary = 1', @where);

	PREPARE stmt FROM @query;
	EXECUTE stmt;
	DEALLOCATE PREPARE stmt;

	SELECT
        component.pk_component, 
		component.name as component_name,
		version.pk_version,
		version.version,
        test_plan.pk_test_plan,
		test_plan.name as test_plan_name,
		count(test_instances.pk_test_instance) as total,
		sum(case
			when
				test_instances.pk_test_instance is not null
					and run.passed is null
			then
				1
			else 0
		end) as pending,
		sum(case
			when run.passed = 1 then 1
			else 0
		end) as passed,
		sum(case
			when run.passed = 0 then 1
			else 0
		end) as failed
	FROM
		test_instances
		LEFT JOIN run ON run.pk_run = test_instances.fk_run
		JOIN test ON test.pk_test = test_instances.fk_test
		JOIN test_plan ON test_plan.pk_test_plan = test.fk_test_plan
		JOIN version ON version.pk_version = test_instances.pk_version
		JOIN component ON component.pk_component = version.fk_component
	GROUP BY version.pk_version, test_plan.pk_test_plan
    ORDER BY component_name, version.sort_order, test_plan_name;

	DROP TEMPORARY TABLE IF EXISTS test_instances;



END$$

DELIMITER ;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
