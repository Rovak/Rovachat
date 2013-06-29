"use strict";

var websocket = new WebSocket("ws://localhost:9000/chat/room");

websocket.onopen = function (ev) {
    console.log("websocket open");
    websocket.send(JSON.stringify({ action: 'auth', username: 'Anonymous' }));
    websocket.send(JSON.stringify({ action: 'channels' }));
};

websocket.onclose = function (ev) {

};


websocket.onerror = function (ev) {

};

function addChannel(name) {
    websocket.send(JSON.stringify({
        action: 'addchannel',
        name: name
    }));
}

var currentChannel = 'Public';

function ChatChannelCtrl($scope, $routeParams, $route) {

    $scope.onMessage = function() {
        var msg = $scope.message;
        if (!msg) return;

        if (msg.substr(0,5) == '/nick') {
            websocket.send(JSON.stringify({
                'action' : 'auth',
                'username' : msg.substr(6)
            }));
            $scope.message = "";
            return;
        }

        websocket.send(JSON.stringify({
            'action' : 'channel',
            'channel' : currentChannel,
            'message' : $scope.message
        }));
        $scope.message = "";
    };
}

function ChannelCtrl($scope) {

    $scope.channels = [];
    $scope.users = [];

    $scope.switchChannel = function(channel) {
        currentChannel = channel;
        $('#channels .channel').hide();
        $('#room-' + channel).show();
        $('li.channel a.active').removeClass('active');
        $('li.channel a[channel="' + channel + '"]').addClass('active');
        $('.chat-input').focus();
    };

    websocket.onmessage = function(ev) {

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
                    var divChannel = $('#room-' + data.channel);

                    if (!divChannel.length) {
                        $('#channels').append('<div class="channel" id="room-' + data.channel + '"></div>');
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

