'use strict';

module.exports = function (grunt) {

  grunt.initConfig({
    compass: {
      dev: {
        options: {
          sassDir: 'public/sass',
          cssDir: 'public/css'
        }

      }
    },
    watch: {
      scripts: {
        files: ['**/*.scss'],
        tasks: ['compass:dev']
      }
    }
  });

  grunt.loadNpmTasks('grunt-contrib-compass');
  grunt.loadNpmTasks('grunt-contrib-watch');

  grunt.registerTask('default', ['jshint', 'compass']);
};