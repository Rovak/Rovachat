var rovachatModule = angular.module('rovachat', []);


rovachatModule.service('live', function() {

    var websocket = new WebSocket(config.websocketUrl);

    websocket.onopen = function (ev) {
        console.log("websocket open");
        websocket.send(JSON.stringify({ action: 'auth', username: 'Anonymous' }));
        websocket.send(JSON.stringify({ action: 'channels' }));
    };

    websocket.onclose = function (ev) {

    };


    websocket.onerror = function (ev) {

    };

    return {
        socket: websocket,
        sendAction: function(action, obj) {
            _.extend(obj, { action: action });
            websocket.send(JSON.stringify(obj));
        }
    };
});


rovachatModule.service('screen', function() {
    return {
        channelsContainer: function() {
            return $('#channels-containers .channel');
        },
        getChannelMenuItemEl: function(channel) {
            return $('li.channel a[channel="' + channel + '"]');
        },
        getChannelEl: function(channel) {
            return $('#room-' + channel);
        },
        getInput: function() {
            return $('.chat-input');
        }
    };
});



var currentChannel = 'Public';

function ChatChannelCtrl($scope, $routeParams, $route, live) {

    function joinChannel(name) {
        live.sendAction('joinchannel', {
            name: name
        });
    }

    function IsCommand(message) {
        return message[0] == '/';
    }

    function HandleCommand(message) {
        var commands = message.substr(1).split(' ');
        var data = commands.slice(1, commands.length).join(' ');

        switch(commands[0].toLowerCase()) {
            case 'nick':
                live.sendAction('auth', {
                    username: data
                });
                break;
            case 'join':
                live.sendAction('channel_join', {
                    name: data
                });
                break;
            case 'leave':
                live.sendAction('channel_leave', {
                    name: data
                });
                break;
        }
    }

    $scope.onMessage = function() {

        var msg = $scope.message;

        if (!msg) return;

        if (IsCommand(msg)) {
            HandleCommand(msg);
        } else {
            live.sendAction('channel_send', {
                'channel' : currentChannel,
                'message' : $scope.message
            });
        }

        $scope.message = "";
    };
}

function ChannelCtrl($scope, live, screen) {

    $scope.channels = [];
    $scope.users = [];

    $scope.switchChannel = function(channel) {
        currentChannel = channel;
        $('#channels-containers .channel').hide();
        screen.getChannelEl(channel).show();
        $('li.channel a.active').removeClass('active');
        screen.getChannelMenuItemEl(channel).addClass('active');
        screen.getInput().focus();
    };

    live.socket.onmessage = function(ev) {

        var data = JSON.parse(ev.data);

        if (data.action) {
            switch(data.action) {
                case 'users':
                    var users = [];
                    for (var i = 0; i < data.users.length; i++) {
                        users.push({
                            id: data.users[i].id,
                            name: data.users[i].name
                        });
                    }

                    $scope.$apply(function(){
                        $scope.users = users;
                    });
                    break;
                case 'channels':
                    var channels = [];
                    for (var i = 0; i < data.channels.length; i++) {
                        channels.push({
                            title: data.channels[i].name
                        });
                    }

                    $scope.$apply(function(){
                        $scope.channels = channels;
                    });
                    break;
                case 'channel':
                    console.log("reieved message", data);
                    var divChannel = $('#room-' + data.channel);

                    if (!divChannel.length) {
                        $('#channels-containers').append('<div class="channel" id="room-' + data.channel + '"></div>');
                        divChannel = $('#room-' + data.channel);
                    }

                    divChannel.append(
                        '<div class="message">' +
                            '<span class="user">' +
                                data.user +
                            '</span>' +
                            '<span class="text">' +
                                data.message +
                            '</span>' +
                        "</div>");
                    divChannel.scrollTop(divChannel[0].scrollHeight);
                    break;
            }
        }
    };
}