(function() {
    'use strict';

    angular
        .module('beecowGatewayApp', [
            'ngStorage',
            'tmh.dynamicLocale',
            'pascalprecht.translate',
            'ngResource',
            'ngCookies',
            'ngAria',
            'ngCacheBuster',
            'ngFileUpload',
            'ui.bootstrap',
            'ui.bootstrap.datetimepicker',
            'ui.router',
            'infinite-scroll',
            // jhipster-needle-angularjs-add-module JHipster will add new module here
            'angular-loading-bar'
        ])
        .run(run);

    run.$inject = ['stateHandler', 'translationHandler','$rootScope', '$state', '$stateParams'];

    function run(stateHandler, translationHandler,$rootScope, $state, $stateParams) {
        stateHandler.initialize();
        translationHandler.initialize();
        $rootScope.$state = $state;
        $rootScope.$stateParams = $stateParams;
        console.log($rootScope.$state)
    }
})();
