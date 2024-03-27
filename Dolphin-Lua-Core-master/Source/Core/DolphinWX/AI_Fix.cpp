#pragma once

#include "DolphinWX/AI_Fix.h";
#include <winsock.h>
#include <stdio.h>

#pragma comment(lib, "ws2_32.lib") // Winsock Library


// --------------------- Helper ---------------------
inline void reverse(char arr[], int start, int end)
{
	while (start < end)
	{
		int temp = arr[start];
		arr[start] = arr[end];
		arr[end] = temp;
		start++;
		end--;
	}
}

inline void s(SOCKET client, char *p, int len)
{
	reverse(p, 0, len - 1);

	int r = send(client, p, len, 0);
	if (r != len)
	{
		printf("send(): could only send %d / %d bytes\n", r, len);
	}
}

inline void r(SOCKET client, char *p, int len)
{
	int got = 0;
	while (got < len)
	{
		char byte = 0;
		int r = recv(client, &byte, 1, 0);
		if (r != 1)
		{
			printf("recv(): expected %d bytes but got %d\n", len, r);
		}
		p[len - got - 1] = byte;
		got++;
	}
}

// --------------------- Read / Sent ---------------------

inline int readInt(SOCKET client)
{
	int v = 0;
	r(client, (char *)&v, sizeof(int));
	return v;
}

inline double readDouble(SOCKET client)
{
	double v = 0;
	r(client, (char *)&v, sizeof(double));
	return v;
}

inline void sendInt(SOCKET client, int value)
{
	s(client, (char *)&value, sizeof(int));
}

inline void sendDouble(SOCKET client, double value)
{
	s(client, (char *)&value, sizeof(double));
}

namespace AI
{
	bool skipRendering;
	int id;
    SOCKET client;

	void AI::SocketSendInt(int value) {
	    sendInt(client, value);
	}

	void AI::SocketSendDouble(double value) {
	    sendDouble(client, value);
	}

	int AI::SocketReadInt() {
	    return readInt(client);
	}

	double AI::SocketReadDouble() {
	    return readDouble(client);
	}

	void AI::SocketReadString(char *buffer, int maxSize)
    {
	    const int size = AI::SocketReadInt();
	    const int r = recv(client, buffer, size <= maxSize ? size : maxSize, 0);
	    buffer[r] = '\0'; // Terminate String
    }

	void AI::socket_init() {
	    WSADATA wsaData;
	    int iResult = WSAStartup(MAKEWORD(2, 2), &wsaData);
	    if (iResult != 0)
	    {
		    printf("error at WSASturtup\n");
		    return;
	    }

	    // Socket creation
	    client = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
	    if (client < 0)
	    {
		    printf("socket creation failed.\n");
		    closesocket(client);
		    WSACleanup();
		    return;
	    }

	    // Server address construction
	    struct sockaddr_in sad;
	    memset(&sad, 0, sizeof(sad));
	    sad.sin_family = AF_INET;
	    sad.sin_addr.s_addr = inet_addr("127.0.0.1"); // server IP
	    sad.sin_port = htons(7850 + id);             // Server port
	    // Connection to the server
	    if (connect(client, (struct sockaddr *)&sad, sizeof(sad)) < 0)
	    {
		    printf("Failed to connect.\n");
		    closesocket(client);
		    WSACleanup();
		    return;
	    }
	}
}