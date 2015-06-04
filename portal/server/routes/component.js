// Mysql Connection
var mysql   = require('../lib/mysql');
var env    = process.env.NODE_ENV || 'development';
var config = require('../config/config')[env];
var squel  = require('squel');

// [GET] List of components
exports.list = function (req, res) {
  var after_id = 0;
  var filter_str = req.param('filter');
  if (req.param('after')) {
    after_id = req.param('after');
  }

  /**
   * Get List of components
   */
  var sql_query =
    squel.select()
    .field('component.pk_component')
    .field('component.name')
    .field('COUNT(DISTINCT test_plans.pk_test_plan)', 'test_plans')
    .field('COUNT(DISTINCT tests.pk_test)', 'tests')
    .from('component')
    .left_join('component_to_test_plan',
      null,
      'component_to_test_plan.fk_component = component.pk_component')
    .left_join('test_plan',
      'test_plans',
      'test_plans.pk_test_plan = component_to_test_plan.fk_test_plan')
    .left_join('test', 'tests', 'tests.fk_test_plan = test_plans.pk_test_plan');

  // Expression for search
  var exp = squel.expr();
  if (filter_str) {
    exp.and('component.pk_component > ?')
      .and("component.name LIKE ?");
    sql_query.where(exp, after_id, "%" + filter_str.replace(/["']/g, "") + "%");
  } else {
    exp.and('component.pk_component > ?');
    sql_query.where(exp, after_id);
  }

  /**
   * Group by
   */
  sql_query.group('component.pk_component');

  /**
   * Order by sort parameter
   */
  switch(req.param('sort_by')) {
    case 'test_plans':
      sql_query.order('test_plans',false);
      break;
    case 'tests':
      sql_query.order('tests',false);
      break;
    case 'name':
      sql_query.order('component.name');
      break;
    case 'associated':
    case 'id':
    default:
      sql_query.order('component.pk_component');
  }

  /**
   * Limit of records by global page_limit
   * All parameter being true means no limit
   */
  if (!req.param('all') || req.param('all') != 'true') {
    sql_query.limit(config.page_limit);
  }

  mysql.getConnection(function(err,conn) {
    conn.query(sql_query.toString(),
      function (err, result) {
        if (err) throw err;
        res.format({
          'text/html': function () {
            res.send(result);
          },
          'application/json': function () {
            res.send(result);
          }
        });
      }
    );

    conn.release();
  });
};

// [GET] Individual component
exports.show = function (req, res) {
  var after_id = req.param('after') || 0;
  var filter_str = req.param('filter');

  /**
   * Get component
   */
  var get_comp = squel.select()
    .field('pk_component')
    .field('name')
    .from('component')
    .where('pk_component = ?', req.param('id'));

  mysql.getConnection(function(err,conn) {
    conn.query(
        get_comp.toString(),
      function (err, result) {
        if (err) throw err;

        var sql_query = squel.select()
          .field('test_plan.name')
          .field('test_plan.pk_test_plan')
          .field('test_plan.description')
          .field('component_to_test_plan.fk_component')
          .field('COUNT(DISTINCT test.pk_test)', 'tests')
          .from('test_plan')
          .left_join('component_to_test_plan', null,
            'component_to_test_plan.fk_test_plan = test_plan.pk_test_plan ' +
            'AND component_to_test_plan.fk_component = '+conn.escape(req.param('id')))
          .left_join('test', null, 'test.fk_test_plan = test_plan.pk_test_plan');

        /**
         * Expression for like search
         * by filter_str
         */
        var exp = squel.expr();
        if (filter_str) {
          exp.and('test_plan.pk_test_plan > ?')
            .and('test_plan.name LIKE ?');
          sql_query.where(exp,
            after_id, "%" + filter_str.replace(/["']/g, "") + "%");
        } else {
          exp.and('test_plan.pk_test_plan > ?');
          sql_query.where(exp, after_id);
        }

        /**
         * Group by
         */
        sql_query.group('test_plan.pk_test_plan');

        /**
         * Order by sort parameter
         */
        switch(req.param('sort_by')) {
          case 'tests':
            sql_query.order('tests',false);
            break;
          case 'name':
            sql_query.order('test_plan.name');
            break;
          case 'associated':
            sql_query.order('fk_component',false);
            break;
          case 'id':
          default:
            sql_query.order('test_plan.pk_test_plan');
        }

        /**
         * Limit of records by global page_limit
         */
        sql_query.limit(config.page_limit);

        /**
         * Get test plans
         */
        conn.query(sql_query.toString(),
          function (tp_err, tp_result) {
            res.format({
              'text/html': function () {
                res.send({ component: result[0], test_plans: tp_result });
              },
              'application/json': function () {
                res.send({ component: result[0], test_plans: tp_result });
              }
            });
          });
      }
    );
    conn.release();
  });
};

// [DELETE] test plan from component
exports.remove_test_plan = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query('DELETE FROM component_to_test_plan ' +
        'WHERE fk_component = ? AND fk_test_plan = ?',
      [req.param('fk_component'), req.param('fk_test_plan')],
      function (err, result) {
        if (err) throw err;
        res.format({
          'text/html': function () {
            res.send(result);
          },
          'application/json': function () {
            res.send(result);
          }
        });
      }
    );
    conn.release();
  });
};

// [POST] test plan to component
exports.add_test_plan = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query('INSERT INTO component_to_test_plan SET ?', req.body,
      function (err, result) {
        if (err) throw err;
        res.format({
          'text/html': function () {
            res.send(result);
          },
          'application/json': function () {
            res.send(result);
          }
        });
      }
    );
    conn.release();
  });
};

// [POST] new component
exports.create = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query('INSERT INTO component SET ?', req.body,
      function (err, result) {
        if (err) throw err;
        res.format({
          'text/html': function () {
            res.send(result);
          },
          'application/json': function () {
            res.send(result);
          }
        });
      }
    );
    conn.release();
  });
};

// [POST] Update component by pk_component
exports.update = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query(
        'UPDATE component SET name = ? ' +
        'WHERE pk_component = ? ',
      [req.body['name'], req.body['pk_component']],
      function (err, result) {
        if (err) throw err;
        res.format({
          'text/html': function () {
            res.send(result)
          },
          'application/json': function () {
            res.send(result);
          }
        });
      }
    );
    conn.release();
  });
};

// [DELETE] component by pk_component
exports.destroy = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query('DELETE FROM component WHERE pk_component = ?', req.param('id'),
      function (err, result) {
        if (err) throw err;
        res.format({
          'text/html': function () {
            res.send(result);
          },
          'application/json': function () {
            res.send(result);
          }
        });
      }
    );
    conn.release();
  });
};
