# dhcp-routing
make DHCP become unicast with ONOS</br>

DHCP: isc-dhcp-server</br>
ONOS: 1.14</br>
mininet: 2.2.1</br>

1. DHCPsecond.py</br>
	It creates two switches(s1, s2) and three hosts(h1, h2, dhcp).</br>
	h1 and h2 link to s1.</br>
	dhcp links to s2.</br>
	h1 will get dynamic IP from dhcp.</br>
	h2 has static IP.</br>
	We can open wireshark on h2 to see whether h2 receives dhcp packets.</br>
	
	Because it starts dhcp server by calling "dhcpd"(isc-dhcp-server daemon) to bypass namespace, we need to delete "dhcpd.pid" and "dhclient.pid" in /var/run or we can't start it again after closing it.</br>
	Before starting it, we need to change the access right of "dhcpd.leases" in /var/lib/dhcp to 777.</br>

2. dhcp-routing.json</br>
	It tells the processor where dhcp is.</br>

3. dhpd.conf</br>
	It contains the configuration of dhcp server.</br>
