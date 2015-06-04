function Sort (cb) {
  this.cb = cb;
}

Sort.prototype.by = function (value) {
  this.cb(value);
};

module.exports = Sort;