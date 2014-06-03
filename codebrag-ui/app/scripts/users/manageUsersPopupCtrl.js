angular.module('codebrag.userMgmt')

    .controller('ManageUsersPopupCtrl', function($scope, userMgmtService, licenceService, popupsService, Flash) {

        $scope.flash = new Flash();

        userMgmtService.loadUsers().then(function(users) {
            $scope.users = users;
        });

        licenceService.ready().then(function(licenceData) {
            $scope.licenceMaxUsers = licenceData.maxUsers;
        });

        $scope.countActiveUsers = function() {
            return $scope.users ? $scope.users.filter(function(user) { return user.active; }).length : 0;
        };

        $scope.invite = function() {
            popupsService.openInvitePopup();
        };

        $scope.modifyUser = function(user) {
            $scope.flash.clear();
            var userData = { userId: user.id, active: user.active, admin: user.admin };
            user.locked = true;
            userMgmtService.modifyUser(userData).then(changed, error).then(function() {
                delete user.locked;
            });

            function changed() {
                $scope.flash.add('info', 'User details changed');
            }

            function error() {
                $scope.flash.add('error', 'Could not change user details');
            }
        };

        $scope.askForNewPassword = function(user) {
            $scope.flash.clear();
            var modal = popupsService.openSetUserPasswordPopup(user);
            modal.result.then(function() {
                $scope.flash.add('info', 'User password changed');
            });
        };

    });
