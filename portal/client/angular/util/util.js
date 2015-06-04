function Util (cb) {}

/**
 * Join array by property
 * Returns string of properties comma separated.
 *
 * @param property
 * @param array
 */
Util.prototype.join_by = function (property,array) {
  var tmp_arr = [];
  for (var i = 0; i < array.length; i++) {
    tmp_arr.push(array[i][property]);
  }
  // return joined string
  return tmp_arr.join();
};

module.exports = Util;