#!/usr/bin/python

from mininet.cli import CLI
from mininet.net import Mininet
from mininet.node import RemoteController
from mininet.log import info, setLogLevel

def myNetwork():
	
	net = Mininet()
	
	# add controller
	info( '*** Adding controller\n' )
	c1 = RemoteController('c1', '127.0.0.1', 6653)
	
	# add switch
	info( '*** Adding switches\n' )
	s1 = net.addSwitch('s1')
	s2 = net.addSwitch('s2')
	
	# add host
	info( '*** Adding hosts\n' )
	h1 = net.addHost('h1', ip = '0.0.0.0')
	h2 = net.addHost('h2', ip = '0.0.0.0')
	dhcp = net.addHost('dhcp', ip = '0.0.0.0')
	
	# link switch and host
	info( '*** Adding links\n' )
	net.addLink(s1, s2)
	net.addLink(h1, s1)
	net.addLink(h2, s1)
	net.addLink(dhcp, s2)
	
	# start network
	info( '*** Starting network\n' )
	net.build()
	c1.start()
	s1.start([c1])
	s2.start([c1])

	# config host 2
	info( '*** Configuring host 2\n' )
	h2.cmd('ifconfig h2-eth0 192.168.44.2 netmask 255.255.255.0')

	# start dhcp server
	info( '*** Starting DHCP server\n' )
	dhcp.cmd('ifconfig dhcp-eth0 192.168.44.1 netmask 255.255.255.0')
	dhcp.cmd('dhcpd')

	# get dynamic IP
	info( '*** Getting dynamic IP\n' )
	intf1 = net.get('h1').defaultIntf()
	intf1.cmd('dhclient h1-eth0')
	intf1.updateIP()
	
	CLI(net)
	
	s1.stop()
	s2.stop()
	net.stop()

if __name__ == '__main__':
	setLogLevel( 'info' )
	myNetwork()
