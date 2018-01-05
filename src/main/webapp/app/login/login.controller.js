(function() {
    'use strict';

    angular
        .module('beecowGatewayApp')
        .controller('LoginMainController', LoginMainController);

    LoginMainController.$inject = ['$rootScope', '$state', '$timeout', 'Auth','$scope', 'Principal'];

    function LoginMainController ($rootScope, $state, $timeout, Auth, $scope, Principal) {
        var vm = this;

        vm.authenticationError = false;
        vm.credentials = {};
        vm.login = login;
        vm.password = null;
        vm.register = register;
        vm.rememberMe = true;
        vm.requestResetPassword = requestResetPassword;
        vm.username = null;

        $timeout(function (){angular.element('#username').focus();});

        function login (event) {
            event.preventDefault();
            Auth.login({
                username: vm.username,
                password: vm.password,
                rememberMe: vm.rememberMe
            }).then(function () {
                vm.authenticationError = false;
                if ($state.current.name === 'register' || $state.current.name === 'activate' ||
                    $state.current.name === 'finishReset' || $state.current.name === 'requestReset') {
                    console.log("abc1233");
                    $state.go('home');
                }

                $rootScope.$broadcast('authenticationSuccess');

                $state.go('settings');
                console.log("abc12354543");
                // previousState was set in the authExpiredInterceptor before being redirected to login modal.
                // since login is successful, go to stored previousState and clear previousState
                if (Auth.getPreviousState()) {
                    console.log("abc");
                    var previousState = Auth.getPreviousState();
                    Auth.resetPreviousState();
                    $state.go(previousState.name, previousState.params);
                }
            }).catch(function () {
                vm.authenticationError = true;
            });
        }

        function register () {
            //$state.go('register');
            $state.go('settings');
        }

        function requestResetPassword () {
            $state.go('requestReset');
        }
    }
})();
