# Assignment report notes

CloudWare Pro can be described as a cloud-based ERP/CRM/WMS platform for a wholesale clothing company. The web gateway represents the public subnet. The backend API containers represent private application services. PostgreSQL represents the private database subnet. Nginx distributes requests across two backend instances, which demonstrates load balancing and basic scaling.

For screenshots, open the Login page, Dashboard, Orders, Inventory, Customers and Cloud Network page. Then run `/api/network/instance` several times to prove traffic is distributed between backend instances.
