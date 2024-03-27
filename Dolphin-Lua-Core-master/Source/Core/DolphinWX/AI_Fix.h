#pragma once


namespace AI
{
	extern bool skipRendering;
	extern int id;

	void socket_init();

	void SocketSendInt(int value);
    void SocketSendDouble(double value);
    int SocketReadInt();
    double SocketReadDouble();
    void SocketReadString(char *buffer, int size);
    }