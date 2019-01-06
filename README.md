# dhcp-routing
make DHCP become unicast with ONOS

DHCP: isc-dhcp-server</br>
ONOS: 1.14
mininet: 2.2.1

1. DHCPsecond.py
	It creates two switches(s1, s2) and three hosts(h1, h2, dhcp).
	h1 and h2 link to s1.
	dhcp links to s2.
	h1 will get dynamic IP from dhcp.
	h2 has static IP.
2. dhcp-routing.json
	It tells the processor where dhcp is.

3. dhpd.conf
	It contains the configuration of dhcp server.
